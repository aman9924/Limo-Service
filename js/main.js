/* =========================================================
   HONK Limousine Service — Global JS
   ========================================================= */

// Client-side config for the WhatsApp "quick contact" links/buttons only.
// Actual booking notifications are sent server-side (see BookingApiController + WhatsAppService).
const SITE_CONFIG = {
  ownerWhatsAppNumber: "18439296113", // country code + number, no + or spaces
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
});

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
  const params = new URLSearchParams(window.location.search);
  params.forEach((value, key) => {
    const field = form.elements[key];
    if (field) field.value = value;
  });
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
        showToast("success", `Your booking request has been sent. Reference: <strong>${result.bookingReference}</strong> — save it to track or cancel your ride later.`);
        const trackLink = document.getElementById("trackBookingLink");
        if (trackLink) {
          trackLink.href = `/track?ref=${encodeURIComponent(result.bookingReference)}`;
          trackLink.style.display = "block";
        }
        form.reset();
        form.classList.remove("was-validated");
        toggleFields();
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

async function maybeUpdateFareEstimate() {
  const panel = document.getElementById("fareEstimate");
  const vehicleSelect = document.getElementById("vehicleTypeSelect");
  if (!panel || !vehicleSelect) return;
  if (!ADDRESS_STATE.pickup || !ADDRESS_STATE.dropoff) return;

  const pickupInput = document.getElementById("pickupLocationInput");
  const dropoffInput = document.getElementById("dropoffLocationInput");

  renderFareCard(panel, { loading: true });

  try {
    const res = await fetch("/api/estimate", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        pickupLocation: pickupInput ? pickupInput.value : "",
        dropoffLocation: dropoffInput ? dropoffInput.value : "",
        pickupLat: ADDRESS_STATE.pickup.lat,
        pickupLng: ADDRESS_STATE.pickup.lng,
        dropoffLat: ADDRESS_STATE.dropoff.lat,
        dropoffLng: ADDRESS_STATE.dropoff.lng,
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

  panel.innerHTML = `
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