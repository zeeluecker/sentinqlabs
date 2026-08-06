/**
 * Sentinq Command Center
 * ----------------------
 * This file is responsible for five things:
 *
 * 1. Managing small amounts of browser-only demo state.
 * 2. Switching between Command Center views.
 * 3. Calling the Sentinq backend APIs.
 * 4. Translating backend responses into UI state.
 * 5. Rendering mandates, audit events, and shopping results.
 *
 * Important architectural boundary:
 * This JavaScript does not perform Sentinq business logic.
 * It does not build mandates or resolve carts. The Spring Boot backend does that.
 */

// -----------------------------------------------------------------------------
// 1. Browser-only demo state
// -----------------------------------------------------------------------------

const appState = {
  mandates: [],
  auditEvents: [],
  blockedActionCount: 0
};

const PAGE_TITLES = {
  overview: "Command Center",
  agents: "Connected Agents",
  mandates: "Mandates",
  audit: "Audit Timeline",
  shop: "Shopping Agent"
};

// -----------------------------------------------------------------------------
// 2. Cache DOM references used repeatedly
// -----------------------------------------------------------------------------

const elements = {
  navItems: document.querySelectorAll(".nav-item"),
  views: document.querySelectorAll(".view"),
  pageTitle: document.getElementById("pageTitle"),
  platformStatus: document.getElementById("platformStatus"),
  runButton: document.getElementById("runButton"),
  runStatus: document.getElementById("runStatus"),
  shopResults: document.getElementById("shopResults"),
  mandateCount: document.getElementById("mandateCount"),
  overviewMandateCount: document.getElementById("overviewMandateCount"),
  blockedCount: document.getElementById("blockedCount"),
  mandateEmpty: document.getElementById("mandateEmpty"),
  mandateList: document.getElementById("mandateList"),
  auditTimeline: document.getElementById("auditTimeline"),
  mandateResult: document.getElementById("mandateResult"),
  selectedResult: document.getElementById("selectedResult"),
  candidateResults: document.getElementById("candidateResults")
};

const agentSelect =
  document.getElementById("agentSelect");

const agentDescription =
  document.getElementById("agentDescription");

const activeAgentName =
  document.getElementById("activeAgentName");
// -----------------------------------------------------------------------------
// 3. Application startup and event registration
// -----------------------------------------------------------------------------

function initializeApplication() {
  registerNavigationEvents();
  registerShoppingEvents();
  checkPlatformHealth();
  refreshCommandCenter();
}

function registerNavigationEvents() {
  elements.navItems.forEach((navItem) => {
    navItem.addEventListener("click", () => {
      switchView(navItem.dataset.view);
    });
  });

  document.querySelectorAll("[data-go-to]").forEach((button) => {
    button.addEventListener("click", () => {
      switchView(button.dataset.goTo);
    });
  });
}

function registerShoppingEvents() {
  elements.runButton.addEventListener("click", handleRunOrchestration);
}

// -----------------------------------------------------------------------------
// 4. Navigation
// -----------------------------------------------------------------------------

function switchView(viewName) {
  elements.navItems.forEach((navItem) => {
    const isSelected = navItem.dataset.view === viewName;
    navItem.classList.toggle("active", isSelected);
  });

  elements.views.forEach((view) => {
    const isSelected = view.id === `${viewName}View`;
    view.classList.toggle("active", isSelected);
  });

  elements.pageTitle.textContent = PAGE_TITLES[viewName] ?? "Sentinq";
  window.scrollTo({ top: 0, behavior: "smooth" });
}

// -----------------------------------------------------------------------------
// 5. Backend API calls
// -----------------------------------------------------------------------------

/**
 * Calls GET /api/system/health.
 * This is only a connectivity/status check.
 */
async function checkPlatformHealth() {
  try {
    const response = await fetch("/api/system/health");

    if (!response.ok) {
      throw new Error(`Health check failed with status ${response.status}`);
    }

    const health = await response.json();
    const version = health.version ?? "running";

    elements.platformStatus.textContent = `${health.status} · ${version}`;
  } catch (error) {
    console.error("Unable to reach Sentinq health endpoint", error);
    elements.platformStatus.textContent = "Demo mode · API unavailable";
  }
}

/**
 * Handles the Shopping Agent button click.
 *
 * High-level flow:
 * 1. Read and validate form inputs.
 * 2. Build the API request body.
 * 3. POST it to /api/shopping/orchestrate.
 * 4. Store the returned mandate and audit information in browser state.
 * 5. Render the returned mandate, candidate carts, and execution decision.
 */
async function handleRunOrchestration() {
  const formInput = readShoppingForm();

  if (!formInput.isValid) {
    elements.runStatus.textContent = formInput.validationMessage;
    return;
  }

  const requestBody = buildOrchestrationRequest(formInput);

  setOrchestrationLoadingState(true);
  setWorkflowProgress(2);

  try {
    const result = await callShoppingOrchestration(requestBody);

    recordOrchestrationResult(result, formInput.goalText);
    renderShoppingResult(result);

    elements.shopResults.classList.remove("hidden");
    elements.runStatus.textContent = "Orchestration complete.";
    setWorkflowProgress(6);
  } catch (error) {
    console.error("Shopping orchestration failed", error);
    elements.runStatus.textContent = `Request failed: ${error.message}`;
  } finally {
    setOrchestrationLoadingState(false);
  }
}

function readShoppingForm() {
  const goalText = document.getElementById("goalInput").value.trim();
  const budgetDollars = Number(document.getElementById("budgetInput").value);
  const deliveryDeadline = document.getElementById("deadlineInput").value;

  if (!goalText || !budgetDollars || !deliveryDeadline) {
    return {
      isValid: false,
      validationMessage: "Enter a goal, budget, and deadline."
    };
  }

  return {
    isValid: true,
    goalText,
    budgetDollars,
    deliveryDeadline
  };
}

/**
 * Creates the JSON object expected by ShoppingOrchestrationRequest.
 *
 * Current MVP limitation:
 * Several values are intentionally hardcoded until Sentinq retrieves them from
 * the principal profile, agent delegation, and preference services.
 */
function buildOrchestrationRequest(formInput) {
  if (!agentSelect.value) {
    throw new Error(
      "Select an AI agent before running orchestration."
    );
  }

  return {
    principalId:
      "11111111-1111-1111-1111-111111111111",

    agentId:
      agentSelect.value,

    goalText:
      buildGoalText(formInput)
  };
}

function buildGoalText(formInput) {
  return `
    ${formInput.goalText}

    Maximum total budget:
    $${formInput.budgetDollars}

    Required delivery deadline:
    ${formInput.deliveryDeadline}
  `.trim();
}

/**
 * This is the only Shopping Agent backend call in the frontend.
 *
 * The JavaScript calls one orchestration endpoint. The Spring backend then calls:
 * - MandateBuilder
 * - MockMerchantSearchService
 * - LateBindingResolutionService
 *
 * The browser receives the final combined result.
 */
async function callShoppingOrchestration(requestBody) {
  const response = await fetch("/api/shopping/orchestrate", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(requestBody)
  });

  if (!response.ok) {
    const errorBody = await response.text();
    throw new Error(`${response.status}: ${errorBody}`);
  }

  return response.json();
}

function setOrchestrationLoadingState(isLoading) {
  elements.runButton.disabled = isLoading;

  if (isLoading) {
    elements.runStatus.textContent = "Sentinq is resolving the mandate…";
  }
}

// -----------------------------------------------------------------------------
// 6. Translate the backend response into Command Center state
// -----------------------------------------------------------------------------

/**
 * Adds returned data to browser-only state.
 *
 * Today, this simulates persistence. Later, mandates and audit events should come
 * from backend repositories and dedicated APIs.
 */
function recordOrchestrationResult(result, originalGoal) {
  const mandateRecord = {
    ...result.mandate,
    originalGoal,
    createdAt: new Date()
  };

  appState.mandates.unshift(mandateRecord);
  appState.blockedActionCount += countBlockedCandidates(result.candidates);
  appState.auditEvents.unshift(...buildAuditEvents(result, originalGoal));

  refreshCommandCenter();
}

function countBlockedCandidates(candidates = []) {
  return candidates.filter((candidate) => {
    return !candidate.resolution.executable;
  }).length;
}

/**
 * These are UI-generated audit descriptions based on the final API response.
 * They are not yet true backend audit events.
 */
function buildAuditEvents(result, originalGoal) {
  const events = [
    {
      title: "Goal submitted",
      description: originalGoal
    },
    {
      title: "Scoped context applied",
      description: "Consumer merchant and trust preferences were attached to the task."
    },
    {
      title: "Preliminary mandate synthesized",
      description: `Mandate ${shortId(result.mandate.mandateId)} created.`
    },
    {
      title: "Candidate carts resolved",
      description: `${result.candidates?.length ?? 0} candidate carts passed through late-binding resolution.`
    }
  ];

  if (result.selectedCandidate) {
    events.push({
      title: "Executable cart selected",
      description: `${result.selectedCandidate.offer.merchantName} satisfied the hard mandate constraints.`
    });
  } else {
    events.push({
      title: "Execution blocked",
      description: "No candidate cart satisfied all hard constraints."
    });
  }

  return events.map((event) => ({
    ...event,
    time: new Date()
  }));
}
function updateSelectedAgentDescription() {
  const selectedOption =
    agentSelect.options[
      agentSelect.selectedIndex
    ];

  if (!selectedOption ||
      !selectedOption.value) {
    agentDescription.textContent =
      "Select an available AI agent.";

    activeAgentName.textContent =
      "No agent selected";

    return;
  }

  const provider =
    selectedOption.dataset.provider;

  const model =
    selectedOption.dataset.model;

  agentDescription.textContent =
    `Provider: ${provider} · Model: ${model}`;

  activeAgentName.textContent =
    selectedOption.textContent;
}

async function loadAvailableAgents() {
  try {
    const response =
      await fetch("/api/agents");

    if (!response.ok) {
      throw new Error(
        `Unable to load agents: ${response.status}`
      );
    }

    const agents =
      await response.json();

    populateAgentDropdown(agents);
  } catch (error) {
    console.error(
      "Failed to load available agents.",
      error
    );

    agentSelect.innerHTML = `
      <option value="">
        Unable to load agents
      </option>
    `;

    agentSelect.disabled = true;

    agentDescription.textContent =
      "Sentinq could not load the available agents.";
  }
}

function populateAgentDropdown(agents) {
  agentSelect.innerHTML = "";

  if (!Array.isArray(agents) ||
      agents.length === 0) {
    agentSelect.innerHTML = `
      <option value="">
        No active agents available
      </option>
    `;

    agentSelect.disabled = true;

    agentDescription.textContent =
      "No active agents are currently available.";

    activeAgentName.textContent =
      "No agent selected";

    return;
  }

  agents.forEach(agent => {
    const option =
      document.createElement("option");

    option.value =
      agent.agentId;

    option.textContent =
      `${agent.agentName} — ${agent.model}`;

    option.dataset.provider =
      agent.provider;

    option.dataset.model =
      agent.model;

    option.dataset.agentName =
      agent.agentName;

    agentSelect.appendChild(option);
  });

  agentSelect.disabled = false;

  updateSelectedAgent();
}

function updateSelectedAgent() {
  const selectedOption =
    agentSelect.options[
      agentSelect.selectedIndex
    ];

  if (!selectedOption ||
      !selectedOption.value) {
    agentDescription.textContent =
      "Select an available AI agent.";

    activeAgentName.textContent =
      "No agent selected";

    return;
  }

  const provider =
    selectedOption.dataset.provider;

  const model =
    selectedOption.dataset.model;

  const agentName =
    selectedOption.dataset.agentName;

  agentDescription.textContent =
    `Provider: ${provider} · Model: ${model}`;

  activeAgentName.textContent =
    agentName;
}

agentSelect.addEventListener(
  "change",
  updateSelectedAgent
);

document.addEventListener(
  "DOMContentLoaded",
  () => {
    loadAvailableAgents();
  }
);
// -----------------------------------------------------------------------------
// 7. Command Center rendering
// -----------------------------------------------------------------------------

function refreshCommandCenter() {
  const mandateCount = appState.mandates.length;

  elements.mandateCount.textContent = mandateCount;
  elements.overviewMandateCount.textContent = mandateCount;
  elements.blockedCount.textContent = appState.blockedActionCount;

  renderMandateList();
  renderAuditTimeline();
}

function renderMandateList() {
  if (appState.mandates.length === 0) {
    elements.mandateEmpty.classList.remove("hidden");
    elements.mandateList.classList.add("hidden");
    return;
  }

  elements.mandateEmpty.classList.add("hidden");
  elements.mandateList.classList.remove("hidden");

  elements.mandateList.innerHTML = appState.mandates
    .map(createMandateListItemHtml)
    .join("");
}

function createMandateListItemHtml(mandate) {
  return `
    <article class="mandate-row">
      <div class="mandate-row-top">
        <div>
          <h3>${escapeHtml(mandate.objective)}</h3>
          <p>${escapeHtml(mandate.originalGoal)}</p>
        </div>
        <span class="badge badge-accent">Synthesized</span>
      </div>
      <div class="mandate-meta">
        <span>${formatMoney(mandate.maximumTotalCents)} maximum</span>
        <span>Delivery by ${formatDate(mandate.deliveryDeadline)}</span>
        <span>ID ${shortId(mandate.mandateId)}</span>
      </div>
    </article>
  `;
}

function renderAuditTimeline() {
  if (appState.auditEvents.length === 0) {
    return;
  }

  elements.auditTimeline.innerHTML = appState.auditEvents
    .map(createAuditEventHtml)
    .join("");
}

function createAuditEventHtml(auditEvent) {
  const displayTime = auditEvent.time.toLocaleTimeString([], {
    hour: "numeric",
    minute: "2-digit"
  });

  return `
    <div class="audit-event">
      <span class="audit-dot"></span>
      <div>
        <strong>${escapeHtml(auditEvent.title)}</strong>
        <p>${escapeHtml(auditEvent.description)}</p>
        <time>${displayTime}</time>
      </div>
    </div>
  `;
}

function setWorkflowProgress(completedStepCount) {
  const workflowItems = document.querySelectorAll("#workflowList li");

  workflowItems.forEach((item, index) => {
    item.classList.toggle("complete", index < completedStepCount);
  });
}

// -----------------------------------------------------------------------------
// 8. Shopping result rendering
// -----------------------------------------------------------------------------

function renderShoppingResult(result) {
  renderMandate(result.mandate);
  renderSelectedCandidate(result.selectedCandidate);
  renderCandidateCarts(result.candidates ?? []);
}

function renderMandate(mandate) {
  elements.mandateResult.innerHTML = `
    <article class="panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">SYNTHESIZED MANDATE</p>
          <h3>${escapeHtml(mandate.objective)}</h3>
        </div>
        <span class="badge badge-accent">${shortId(mandate.mandateId)}</span>
      </div>

      <div class="summary-grid">
        ${createSummaryItemHtml("Maximum total", formatMoney(mandate.maximumTotalCents))}
        ${createSummaryItemHtml("Delivery deadline", formatDate(mandate.deliveryDeadline))}
        ${createSummaryItemHtml("Substitutions", mandate.substitutionsAllowed ? "Allowed" : "Not allowed")}
        ${createSummaryItemHtml("New merchant", mandate.askBeforeUsingNewMerchant ? "Ask first" : "Permitted")}
      </div>
    </article>
  `;
}

function createSummaryItemHtml(label, value) {
  return `
    <div class="summary-item">
      <span>${escapeHtml(label)}</span>
      <strong>${escapeHtml(value)}</strong>
    </div>
  `;
}

function renderSelectedCandidate(selectedCandidate) {
  if (!selectedCandidate) {
    elements.selectedResult.innerHTML = createBlockedDecisionHtml();
    return;
  }

  elements.selectedResult.innerHTML = createSelectedCandidateHtml(selectedCandidate);
}

function createBlockedDecisionHtml() {
  return `
    <article class="panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">EXECUTION DECISION</p>
          <h3>No executable cart</h3>
        </div>
        <span class="badge badge-danger">Blocked</span>
      </div>
      <p class="section-copy">
        Sentinq prevented execution because every candidate violated at least one hard mandate constraint.
      </p>
    </article>
  `;
}

function createSelectedCandidateHtml(selectedCandidate) {
  const { offer, resolution } = selectedCandidate;

  return `
    <article class="panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">SELECTED CART</p>
          <h3>${escapeHtml(offer.merchantName)}</h3>
        </div>
        <span class="badge badge-success">Executable</span>
      </div>
      <p class="section-copy">
        ${escapeHtml(offer.productName)} ·
        ${formatMoney(resolution.resolvedTotalCents)} ·
        delivery ${formatDate(resolution.estimatedDeliveryDate)}
      </p>
    </article>
  `;
}

function renderCandidateCarts(candidates) {
  elements.candidateResults.innerHTML = `
    <div class="section-heading">
      <div>
        <p class="eyebrow">LATE-BINDING RESOLUTION</p>
        <h2>Candidate carts</h2>
      </div>
    </div>
    <div class="candidate-grid">
      ${candidates.map(createCandidateCardHtml).join("")}
    </div>
  `;
}

function createCandidateCardHtml(candidate) {
  const { offer, resolution } = candidate;
  const statusClass = resolution.executable ? "badge-success" : "badge-danger";
  const statusLabel = resolution.executable ? "Executable" : "Blocked";

  return `
    <article class="candidate-card">
      <div class="candidate-header">
        <div>
          <h3>${escapeHtml(offer.merchantName)}</h3>
          <p>${escapeHtml(offer.productName)}</p>
        </div>
        <span class="badge ${statusClass}">${statusLabel}</span>
      </div>

      <div class="price-grid">
        <span>Product</span><strong>${formatMoney(resolution.productPriceCents)}</strong>
        <span>Shipping</span><strong>${formatMoney(resolution.shippingCents)}</strong>
        <span>Tax</span><strong>${formatMoney(resolution.taxCents)}</strong>
        <span>Resolved total</span><strong>${formatMoney(resolution.resolvedTotalCents)}</strong>
      </div>

      <p class="section-copy">
        Delivery ${formatDate(resolution.estimatedDeliveryDate)} ·
        fulfillment ${offer.fulfillmentScore} ·
        review ${offer.reviewScore}
      </p>

      ${createMessageBoxHtml("Violations", resolution.violations, "danger")}
      ${createMessageBoxHtml("Warnings", resolution.warnings, "warning")}
    </article>
  `;
}

function createMessageBoxHtml(title, messages, messageType) {
  if (!messages?.length) {
    return "";
  }

  const messageItems = messages
    .map((message) => `<li>${escapeHtml(message)}</li>`)
    .join("");

  return `
    <div class="message-box ${messageType}">
      <strong>${escapeHtml(title)}</strong>
      <ul>${messageItems}</ul>
    </div>
  `;
}

// -----------------------------------------------------------------------------
// 9. Formatting and safety helpers
// -----------------------------------------------------------------------------

function formatMoney(cents = 0) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD"
  }).format(cents / 100);
}

function formatDate(dateValue) {
  if (!dateValue) {
    return "—";
  }

  return new Date(`${dateValue}T00:00:00`).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric"
  });
}

function shortId(value) {
  return value ? value.slice(0, 8) : "pending";
}

/**
 * Escapes data before inserting it into innerHTML.
 * This prevents user-controlled text from becoming executable HTML.
 */
function escapeHtml(value) {
  const temporaryElement = document.createElement("div");
  temporaryElement.textContent = value ?? "";
  return temporaryElement.innerHTML;
}

initializeApplication();
