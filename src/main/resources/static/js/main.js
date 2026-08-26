/* =========================================================
   HONK Limousine Service — Global JS
   ========================================================= */

// Client-side config for the WhatsApp "quick contact" links/buttons only.
// Actual booking notifications are sent server-side (see BookingApiController + WhatsAppService).
const SITE_CONFIG = {
  ownerWhatsAppNumber: "13125550100", // country code + number, no + or spaces
  // Public Mapbox token for browser-side autocomplete only — restrict it to this site's
  // domain(s) in your Mapbox account (Tokens > URL restrictions). The Directions API call
  // (distance/fare) runs server-side using a separate MAPBOX_ACCESS_TOKEN env var.
  mapboxPublicToken: "pk.eyJ1IjoiaG9ua2xpbW8iLCJhIjoiY21zencxMDh5MDNwNjJ6cjQybHJmZ3hzbyJ9.SK4kWg0gQKWLtZwE_Bm3zQ",
};

document.addEventListener("DOMContentLoaded", () => {
  initNavbarScroll();
  initHeroQuickBooking();
  initBookingForm();
  initContactForm();
  initTrackForm();
  initAddressAutocomplete();
  setActiveNavLink();
  initDateConstraints();
  initPhoneFormatter();
  initGpsButtons();
  initSwapButton();
  initCapacityValidator();
});

/* ---------- Phone Auto-formatter ---------- */
function initPhoneFormatter() {
  const phoneInputs = document.querySelectorAll('input[type="tel"]');
  phoneInputs.forEach(input => {
    input.addEventListener('input', function (e) {
      let x = e.target.value.replace(/\D/g, '').match(/(\d{0,3})(\d{0,3})(\d{0,4})/);
      e.target.value = !x[2] ? x[1] : '(' + x[1] + ') ' + x[2] + (x[3] ? '-' + x[3] : '');
    });
  });
}

/* ---------- Swap Locations Button ---------- */
function initSwapButton() {
  const swapBtn = document.getElementById("swapLocationsBtn");
  if (!swapBtn) return;
  swapBtn.addEventListener("click", () => {
    const pickupInput = document.getElementById("pickupLocationInput");
    const dropoffInput = document.getElementById("dropoffLocationInput");
    
    // Swap text
    const tempText = pickupInput.value;
    pickupInput.value = dropoffInput.value;
    dropoffInput.value = tempText;

    // Swap state
    const tempState = ADDRESS_STATE.pickup;
    ADDRESS_STATE.pickup = ADDRESS_STATE.dropoff;
    ADDRESS_STATE.dropoff = tempState;

    maybeUpdateFareEstimate();
  });
}

/* ---------- GPS Button Logic ---------- */
function initGpsButtons() {
  const gpsBtns = document.querySelectorAll(".gps-btn");
  gpsBtns.forEach(btn => {
    btn.addEventListener("click", () => {
      const targetId = btn.getAttribute("data-target");
      const stateKey = btn.getAttribute("data-state") || "pickup";
      const input = document.getElementById(targetId);
      
      if (!navigator.geolocation) {
        showToast("error", "Geolocation is not supported by your browser");
        return;
      }
      
      const originalIcon = btn.innerHTML;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>';
      btn.disabled = true;

      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;
          try {
            const url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${lng},${lat}.json?access_token=${SITE_CONFIG.mapboxPublicToken}&types=address,poi`;
            const res = await fetch(url);
            const data = await res.json();
            if (data.features && data.features.length > 0) {
              const placeName = data.features[0].place_name;
              input.value = placeName;
              ADDRESS_STATE[stateKey] = { lat: lat, lng: lng };
              maybeUpdateFareEstimate();
            } else {
              showToast("warning", "Could not determine exact address. Please enter manually.");
            }
          } catch (e) {
            showToast("error", "Failed to retrieve address.");
          } finally {
            btn.innerHTML = originalIcon;
            btn.disabled = false;
          }
        },
        (error) => {
          btn.innerHTML = originalIcon;
          btn.disabled = false;
          let msg = "Could not get location.";
          if (error.code === error.PERMISSION_DENIED) msg = "Location permission denied.";
          showToast("warning", msg);
        }
      );
    });
  });
}

/* ---------- Vehicle Capacity Validation ---------- */
const VEHICLE_CAPACITIES = {
  "sedan": { pax: 3, lug: 3, label: "Executive Sedan" },
  "suv": { pax: 6, lug: 6, label: "Luxury SUV" },
  "sprinter": { pax: 14, lug: 14, label: "Sprinter Van" },
  "stretch": { pax: 10, lug: 4, label: "Stretch Limo" },
  "partybus": { pax: 25, lug: 10, label: "Party Bus" },
  "motorcoach": { pax: 55, lug: 55, label: "Motor Coach" }
};

function initCapacityValidator() {
  const vehicleSelect = document.getElementById("vehicleTypeSelect");
  const paxInput = document.getElementById("passengersInput");
  const lugInput = document.getElementById("luggageInput");
  const warningDiv = document.getElementById("capacityWarning");
  const warningText = document.getElementById("capacityWarningText");

  if (!vehicleSelect || !paxInput || !lugInput || !warningDiv) return;

  function validate() {
    const vKey = vehicleSelect.value;
    const pax = parseInt(paxInput.value) || 0;
    const lug = parseInt(lugInput.value) || 0;
    const limits = VEHICLE_CAPACITIES[vKey];

    if (!limits) {
      warningDiv.style.display = "none";
      return;
    }

    if (pax > limits.pax || lug > limits.lug) {
      warningText.innerHTML = `<strong>Note:</strong> A ${limits.label} comfortably holds up to ${limits.pax} passengers and ${limits.lug} bags. Consider upgrading your vehicle type if your party is larger.`;
      warningDiv.style.display = "block";
    } else {
      warningDiv.style.display = "none";
    }
  }

  vehicleSelect.addEventListener("change", validate);
  paxInput.addEventListener("input", validate);
  lugInput.addEventListener("input", validate);
}

/* ---------- Date Constraints ---------- */
function initDateConstraints() {
  const dateInputs = document.querySelectorAll('input[type="date"]');
  if (dateInputs.length === 0) return;
  
  // Format today as YYYY-MM-DD
  const today = new Date().toLocaleDateString('en-CA'); 
  dateInputs.forEach(input => {
    input.setAttribute('min', today);
  });
}

/* ---------- Navbar shrink on scroll ---------- */
function initNavbarScroll() {
  const navbar = document.querySelector(".navbar-custom");
  if (!navbar) return;
  window.addEventListener("scroll", () => {
    navbar.classList.toggle("scrolled", window.scrollY > 40);
  });
}

/* ---------- Highlight current page in nav ---------- */
function setActiveNavLink() {
  const path = window.location.pathname === "" ? "/" : window.location.pathname;
  document.querySelectorAll(".navbar-custom .nav-link").forEach((link) => {
    const href = link.getAttribute("href");
    if (href === path) link.classList.add("active");
  });
}

/* ---------- Hero quick-booking widget (home page) ---------- */
function initHeroQuickBooking() {
  ["quickBookingForm", "quickBookingFormHourly"].forEach((formId) => {
    const form = document.getElementById(formId);
    if (!form) return;

    form.addEventListener("submit", (e) => {
      e.preventDefault();
      if (!form.checkValidity()) {
        e.stopPropagation();
        form.classList.add("was-validated");
        return;
      }
      const params = new URLSearchParams(new FormData(form));
      window.location.href = `/booking?${params.toString()}`;
    });
  });
}

/* ---------- Prefill full booking form from query params ---------- */
function prefillBookingForm() {
  const form = document.getElementById("bookingForm");
  if (!form) return;
  
  // 1. Prefill from LocalStorage (returning customer)
  const savedName = localStorage.getItem("honk_fullName");
  const savedPhone = localStorage.getItem("honk_phone");
  const savedEmail = localStorage.getItem("honk_email");
  
  if (savedName && form.elements["fullName"]) form.elements["fullName"].value = savedName;
  if (savedPhone && form.elements["phone"]) form.elements["phone"].value = savedPhone;
  if (savedEmail && form.elements["email"]) form.elements["email"].value = savedEmail;

  // 2. Prefill from URL Parameters (from Quick Booking Widget)
  const params = new URLSearchParams(window.location.search);
  params.forEach((value, key) => {
    const field = form.elements[key];
    if (field) field.value = value;
    
    if (key === 'pickupLat' && value) {
      if (!ADDRESS_STATE.pickup) ADDRESS_STATE.pickup = {};
      ADDRESS_STATE.pickup.lat = parseFloat(value);
    }
    if (key === 'pickupLng' && value) {
      if (!ADDRESS_STATE.pickup) ADDRESS_STATE.pickup = {};
      ADDRESS_STATE.pickup.lng = parseFloat(value);
    }
    if (key === 'dropoffLat' && value) {
      if (!ADDRESS_STATE.dropoff) ADDRESS_STATE.dropoff = {};
      ADDRESS_STATE.dropoff.lat = parseFloat(value);
    }
    if (key === 'dropoffLng' && value) {
      if (!ADDRESS_STATE.dropoff) ADDRESS_STATE.dropoff = {};
      ADDRESS_STATE.dropoff.lng = parseFloat(value);
    }
  });

  // If we came from the hero widget with pre-filled locations, trigger estimate instantly
  if (params.has('pickupLocation') && params.has('dropoffLocation')) {
    // Small delay to ensure all fields and DOM are fully initialized
    setTimeout(maybeUpdateFareEstimate, 100);
  }
}

/* ---------- Full booking form — posts to the Spring Boot backend ---------- */
function initBookingForm() {
  const form = document.getElementById("bookingForm");
  if (!form) return;

  prefillBookingForm();

  const serviceType = document.getElementById("serviceType");
  const dropoffGroup = document.getElementById("dropoffGroup");
  const returnGroup = document.getElementById("returnGroup");
  const returnTimeGroup = document.getElementById("returnTimeGroup");

  function toggleFields() {
    const val = serviceType.value;
    dropoffGroup.style.display = val === "hourly" ? "none" : "block";
    returnGroup.style.display = val === "roundtrip" ? "block" : "none";
    returnTimeGroup.style.display = val === "roundtrip" ? "block" : "none";
  }
  if (serviceType) {
    serviceType.addEventListener("change", toggleFields);
    toggleFields();
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    if (!form.checkValidity()) {
      e.stopPropagation();
      form.classList.add("was-validated");
      return;
    }

    const data = Object.fromEntries(new FormData(form).entries());
    const submitBtn = form.querySelector('button[type="submit"]');
    const originalLabel = submitBtn.textContent;
    submitBtn.disabled = true;
    submitBtn.textContent = "Sending...";

    try {
      const res = await fetch("/api/bookings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      const result = await res.json();
      if (res.ok) {
        // Save customer details for frictionless future bookings
        if (data.fullName) localStorage.setItem("honk_fullName", data.fullName);
        if (data.phone) localStorage.setItem("honk_phone", data.phone);
        if (data.email) localStorage.setItem("honk_email", data.email);

        showToast("success", `Redirecting to your digital receipt...`);
        // Immediately redirect to tracking page instead of just clearing form
        setTimeout(() => {
          window.location.href = `/track?ref=${encodeURIComponent(result.bookingReference)}`;
        }, 1500);
      } else {
        showToast("error", result.message);
      }
    } catch (err) {
      showToast("error", "Network error. Please call or WhatsApp us directly.");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = originalLabel;
    }
  });
}

/* ---------- Toast notifications ---------- */
function ensureToastContainer() {
  let container = document.getElementById("appToastContainer");
  if (!container) {
    container = document.createElement("div");
    container.id = "appToastContainer";
    container.className = "app-toast-container";
    document.body.appendChild(container);
  }
  return container;
}

function showToast(type, message, title) {
  const container = ensureToastContainer();
  const icons = { success: "bi-check-circle-fill", error: "bi-x-circle-fill", warning: "bi-exclamation-triangle-fill" };
  const titles = { success: "Success", error: "Something went wrong", warning: "Heads up" };

  const toast = document.createElement("div");
  toast.className = `app-toast toast-${type}`;
  toast.innerHTML =
    `<div class="toast-header-custom">` +
    `<i class="bi ${icons[type] || icons.success} toast-icon"></i>` +
    `<span class="toast-title">${title || titles[type] || titles.success}</span>` +
    `<button type="button" class="btn-close-custom" aria-label="Close">&times;</button>` +
    `</div><div class="toast-body-custom">${message}</div>`;

  const remove = () => {
    toast.style.opacity = "0";
    toast.style.transform = "translateX(20px)";
    setTimeout(() => toast.remove(), 250);
  };
  toast.querySelector(".btn-close-custom").addEventListener("click", remove);

  container.appendChild(toast);
  setTimeout(remove, 7000);
}

/* ---------- Contact page general inquiry form (still client-side wa.me for now) ---------- */
function initContactForm() {
  const form = document.getElementById("contactForm");
  if (!form) return;

  form.addEventListener("submit", (e) => {
    e.preventDefault();
    if (!form.checkValidity()) {
      e.stopPropagation();
      form.classList.add("was-validated");
      return;
    }

    const data = Object.fromEntries(new FormData(form).entries());
    const summary =
      `New Contact Inquiry\n--------------------\n` +
      `Name: ${data.name || "-"}\nEmail: ${data.email || "-"}\n` +
      `Phone: ${data.phone || "-"}\nMessage: ${data.message || "-"}`;

    const waLink = `https://wa.me/${SITE_CONFIG.ownerWhatsAppNumber}?text=${encodeURIComponent(summary)}`;
    window.open(waLink, "_blank");

    showToast("success", "Please hit send in the WhatsApp window that opened.");
    form.reset();
    form.classList.remove("was-validated");
  });
}

/* ---------- Track / cancel a booking (guest, no login) ---------- */
function initTrackForm() {
  const form = document.getElementById("trackForm");
  if (!form) return;

  const params = new URLSearchParams(window.location.search);
  const refParam = params.get("ref");
  if (refParam) form.elements["bookingReference"].value = refParam;

  const resultCard = document.getElementById("trackResult");
  const cancelBtn = document.getElementById("cancelBookingBtn");
  let lastLookup = null;

  function renderResult(data) {
    document.getElementById("resultReference").textContent = data.bookingReference;
    document.getElementById("resultServiceType").textContent = data.serviceType || "-";
    document.getElementById("resultVehicleType").textContent = data.vehicleType || "-";
    document.getElementById("resultPickupLocation").textContent = data.pickupLocation || "-";
    document.getElementById("resultDropoffLocation").textContent = data.dropoffLocation || "-";
    document.getElementById("resultDateTime").textContent = `${data.pickupDate || "-"} ${data.pickupTime || ""}`;

    const badge = document.getElementById("resultStatusBadge");
    badge.textContent = data.bookingStatus;
    const badgeColors = {
      PENDING: "bg-warning text-dark",
      CONFIRMED: "bg-success",
      COMPLETED: "bg-secondary",
      CANCELLED: "bg-danger",
    };
    badge.className = `badge ${badgeColors[data.bookingStatus] || "bg-secondary"}`;

    const cancellable = data.bookingStatus === "PENDING" || data.bookingStatus === "CONFIRMED";
    cancelBtn.style.display = cancellable ? "block" : "none";

    const calBtn = document.getElementById("addToCalendarBtn");
    if (data.bookingStatus === "CONFIRMED") {
      calBtn.style.display = "block";
      calBtn.href = "/api/bookings/" + data.bookingReference + "/calendar";
    } else {
      calBtn.style.display = "none";
    }

    if (data.driverName) {
      document.getElementById("driverDetails").style.display = "block";
      document.getElementById("resultDriverName").textContent = data.driverName;
      const phoneEl = document.getElementById("resultDriverPhone");
      if (data.driverPhone) {
        phoneEl.textContent = data.driverPhone;
        phoneEl.href = "tel:" + data.driverPhone.replace(/[^0-9+]/g, '');
        phoneEl.style.display = "block";
      } else {
        phoneEl.style.display = "none";
      }
    } else {
      document.getElementById("driverDetails").style.display = "none";
    }

    resultCard.style.display = "block";
    resultCard.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  form.addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!form.checkValidity()) {
      e.stopPropagation();
      form.classList.add("was-validated");
      return;
    }

    const data = Object.fromEntries(new FormData(form).entries());
    lastLookup = data;

    try {
      const res = await fetch("/api/bookings/track", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      const result = await res.json();
      if (res.ok) {
        resultCard.style.display = "none";
        showToast("success", "Booking found.");
        renderResult(result);
      } else {
        resultCard.style.display = "none";
        showToast("error", result.message);
      }
    } catch (err) {
      showToast("error", "Network error. Please call or WhatsApp us directly.");
    }
  });

  cancelBtn.addEventListener("click", async () => {
    if (!lastLookup) return;
    if (!confirm("Are you sure you want to cancel this booking?")) return;

    try {
      const res = await fetch("/api/bookings/track/cancel", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(lastLookup),
      });
      const result = await res.json();
      if (res.ok) {
        showToast("success", result.message || "Booking cancelled.");
        renderResult(result);
      } else {
        showToast("error", result.message);
      }
    } catch (err) {
      showToast("error", "Network error. Please call or WhatsApp us directly.");
    }
  });
}

/* ---------- Address autocomplete (Mapbox Geocoding) + live fare estimate ---------- */
const ADDRESS_STATE = {
  pickup: null, // { lat, lng }
  dropoff: null,
};

function initAddressAutocomplete() {
  attachAutocomplete("pickupLocationInput", "pickupSuggestions", "pickup");
  attachAutocomplete("dropoffLocationInput", "dropoffSuggestions", "dropoff");
  attachAutocomplete("heroPickupInput", "heroPickupSuggestions", "heroPickup");
  attachAutocomplete("heroDropoffInput", "heroDropoffSuggestions", "heroDropoff");
  attachAutocomplete("heroHourlyPickupInput", "heroHourlyPickupSuggestions", "heroHourlyPickup");

  const vehicleSelect = document.getElementById("vehicleTypeSelect");
  if (vehicleSelect) {
    vehicleSelect.addEventListener("change", maybeUpdateFareEstimate);
  }

  const pickupInput = document.getElementById("pickupLocationInput");
  if (pickupInput) pickupInput.addEventListener("blur", maybeUpdateFareEstimate);

  const dropoffInput = document.getElementById("dropoffLocationInput");
  if (dropoffInput) dropoffInput.addEventListener("blur", maybeUpdateFareEstimate);
}

function attachAutocomplete(inputId, suggestionsId, stateKey) {
  const input = document.getElementById(inputId);
  const box = document.getElementById(suggestionsId);
  if (!input || !box) return;

  let debounceTimer;

  input.addEventListener("input", () => {
    ADDRESS_STATE[stateKey] = null; // typing invalidates any previous selection
    clearTimeout(debounceTimer);
    const query = input.value.trim();
    if (query.length < 3) {
      box.style.display = "none";
      return;
    }
    debounceTimer = setTimeout(() => fetchSuggestions(query, box, input, stateKey), 300);
  });

  document.addEventListener("click", (e) => {
    if (!box.contains(e.target) && e.target !== input) box.style.display = "none";
  });
}

async function fetchSuggestions(query, box, input, stateKey) {
  if (!SITE_CONFIG.mapboxPublicToken || SITE_CONFIG.mapboxPublicToken === "YOUR_MAPBOX_PUBLIC_TOKEN") {
    return; // Mapbox not configured yet — autocomplete silently disabled, plain text entry still works
  }

  try {
    const url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(query)}.json?access_token=${SITE_CONFIG.mapboxPublicToken}&autocomplete=true&limit=5&country=us&types=poi,address,place`;
    const res = await fetch(url);
    const data = await res.json();

    box.innerHTML = "";
    (data.features || []).forEach((feature) => {
      const item = document.createElement("div");
      item.className = "suggestion-item";
      item.textContent = feature.place_name;
      item.addEventListener("click", () => {
        input.value = feature.place_name;
        ADDRESS_STATE[stateKey] = { lng: feature.center[0], lat: feature.center[1] };
        
        const latInput = document.getElementById(stateKey + "Lat");
        if (latInput) latInput.value = feature.center[1];
        
        const lngInput = document.getElementById(stateKey + "Lng");
        if (lngInput) lngInput.value = feature.center[0];

        box.style.display = "none";
        maybeUpdateFareEstimate();
      });
      box.appendChild(item);
    });
    box.style.display = box.childElementCount ? "block" : "none";
  } catch (err) {
    box.style.display = "none";
  }
}

async function geocodeText(query) {
  if (!query || !SITE_CONFIG.mapboxPublicToken) return null;
  try {
    const url = `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(query)}.json?access_token=${SITE_CONFIG.mapboxPublicToken}&limit=1&country=us&types=poi,address,place`;
    const res = await fetch(url);
    const data = await res.json();
    if (data.features && data.features.length > 0) {
      return {
        lng: data.features[0].center[0],
        lat: data.features[0].center[1],
        place_name: data.features[0].place_name
      };
    }
  } catch (err) {
    console.error("Geocoding failed", err);
  }
  return null;
}

let fareEstimateAbortController = null;

async function maybeUpdateFareEstimate() {
  const panel = document.getElementById("fareEstimate");
  const vehicleSelect = document.getElementById("vehicleTypeSelect");
  if (!panel || !vehicleSelect) return;
  const pickupInput = document.getElementById("pickupLocationInput");
  const dropoffInput = document.getElementById("dropoffLocationInput");
  
  if (!pickupInput || !dropoffInput || !pickupInput.value.trim() || !dropoffInput.value.trim()) return;

  if (fareEstimateAbortController) {
    fareEstimateAbortController.abort();
  }
  fareEstimateAbortController = new AbortController();

  renderFareCard(panel, { loading: true });

  let pLat = ADDRESS_STATE.pickup ? ADDRESS_STATE.pickup.lat : null;
  let pLng = ADDRESS_STATE.pickup ? ADDRESS_STATE.pickup.lng : null;
  let pLoc = pickupInput.value;

  if (!pLat || !pLng) {
    const geo = await geocodeText(pLoc);
    if (geo) {
      pLat = geo.lat;
      pLng = geo.lng;
      pLoc = geo.place_name;
      pickupInput.value = geo.place_name;
      ADDRESS_STATE.pickup = { lat: pLat, lng: pLng };
    }
  }

  let dLat = ADDRESS_STATE.dropoff ? ADDRESS_STATE.dropoff.lat : null;
  let dLng = ADDRESS_STATE.dropoff ? ADDRESS_STATE.dropoff.lng : null;
  let dLoc = dropoffInput.value;

  if (!dLat || !dLng) {
    const geo = await geocodeText(dLoc);
    if (geo) {
      dLat = geo.lat;
      dLng = geo.lng;
      dLoc = geo.place_name;
      dropoffInput.value = geo.place_name;
      ADDRESS_STATE.dropoff = { lat: dLat, lng: dLng };
    }
  }

  try {
    const res = await fetch("/api/estimate", {
      method: "POST",
      signal: fareEstimateAbortController.signal,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        pickupLocation: pLoc,
        dropoffLocation: dLoc,
        pickupLat: pLat,
        pickupLng: pLng,
        dropoffLat: dLat,
        dropoffLng: dLng,
        vehicleType: vehicleSelect.value,
      }),
    });
    const result = await res.json();

    if (!res.ok) {
      renderFareCard(panel, { error: result.message || "Could not calculate an estimate right now." });
      return;
    }

    renderFareCard(panel, { result, vehicleLabel: vehicleSelect.options[vehicleSelect.selectedIndex].text });
  } catch (err) {
    if (err.name === 'AbortError') {
      return; // Ignore aborted requests
    }
    renderFareCard(panel, { error: "Could not calculate an estimate right now." });
  }
}

function renderFareCard(panel, { loading, error, result, vehicleLabel } = {}) {
  panel.style.display = "block";

  if (loading) {
    panel.innerHTML = `
      <div class="fare-estimate-main">
        <div class="fare-icon"><i class="bi bi-hourglass-split"></i></div>
        <div class="fare-info">
          <div class="fare-price">Calculating...</div>
          <div class="fare-meta">Getting your live fare estimate</div>
        </div>
      </div>`;
    return;
  }

  if (error) {
    panel.innerHTML = `
      <div class="fare-estimate-main">
        <div class="fare-icon"><i class="bi bi-exclamation-triangle-fill"></i></div>
        <div class="fare-info">
          <div class="fare-price">Estimate unavailable</div>
          <div class="fare-meta">${error}</div>
        </div>
      </div>`;
    return;
  }

  const isCallForPricing = result.pricingType === "CALL_FOR_PRICING";
  const priceText = isCallForPricing ? "Call for Pricing" : `$${result.estimatedFare}`;
  const detailsId = "fareDetailsPanel";
  
  let mapHtml = "";
  if (result.geometry && SITE_CONFIG.mapboxPublicToken) {
    const encodedGeo = encodeURIComponent(result.geometry);
    // Use path-5+d4af37-1 to draw a gold (d4af37) 5px line with 100% opacity over a dark map
    const mapUrl = `https://api.mapbox.com/styles/v1/mapbox/dark-v11/static/path-5+d4af37-1(${encodedGeo})/auto/600x200@2x?padding=30&access_token=${SITE_CONFIG.mapboxPublicToken}`;
    mapHtml = `<div class="mb-3" style="border-radius: 8px; overflow: hidden; border: 1px solid var(--gray-dark);"><img src="${mapUrl}" alt="Route map" style="width: 100%; height: auto; display: block;"></div>`;
  }

  panel.innerHTML = `
    ${mapHtml}
    <div class="fare-estimate-main">
      <div class="fare-icon"><i class="bi bi-cash-coin"></i></div>
      <div class="fare-info">
        <div class="fare-price">${priceText}</div>
        <div class="fare-meta">${result.distanceMiles} mi &middot; ~${result.durationMinutes} min</div>
      </div>
      <button type="button" class="btn btn-sm btn-outline-gold" id="fareDetailsToggle">View Details</button>
    </div>
    <div id="${detailsId}" style="display:none;">
      <div class="fare-details-body">
        <dl>
          <dt>Vehicle</dt><dd>${vehicleLabel || "-"}</dd>
          <dt>Pricing Type</dt><dd>${isCallForPricing ? "Custom Quote" : result.pricingType === "FLAT" ? "Airport Flat Rate" : "Per-Mile Rate"}</dd>
          <dt>Distance</dt><dd>${result.distanceMiles} miles</dd>
          <dt>Estimated Duration</dt><dd>${result.durationMinutes} minutes</dd>
          <dt>Note</dt><dd>${result.note}</dd>
        </dl>
      </div>
    </div>`;

  const toggleBtn = panel.querySelector("#fareDetailsToggle");
  const detailsPanel = panel.querySelector(`#${detailsId}`);
  toggleBtn.addEventListener("click", () => {
    const isOpen = detailsPanel.style.display === "block";
    detailsPanel.style.display = isOpen ? "none" : "block";
    toggleBtn.textContent = isOpen ? "View Details" : "Hide Details";
  });
}
