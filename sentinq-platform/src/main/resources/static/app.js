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

/**
 * Demo principal used throughout the current MVP.
 *
 * Later this will come from authentication or
 * the active signed-in consumer.
 */
const PRINCIPAL_ID =
    "11111111-1111-1111-1111-111111111111";

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
  trustMapResults: document.getElementById("trustMapResults"),
  candidateResults: document.getElementById("candidateResults")
};

const agentSelect =
  document.getElementById("agentSelect");

const agentDescription =
  document.getElementById("agentDescription");

const activeAgentName =
  document.getElementById("activeAgentName");

  const auditTraceList =
    document.getElementById("auditTraceList");

  const auditTraceCount =
    document.getElementById("auditTraceCount");

  const auditTraceEmpty =
    document.getElementById("auditTraceEmpty");

  const auditTraceDetail =
    document.getElementById("auditTraceDetail");

  const auditTraceTitle =
    document.getElementById("auditTraceTitle");

  const auditTraceSubtitle =
    document.getElementById("auditTraceSubtitle");

  const auditTraceStatus =
    document.getElementById("auditTraceStatus");

  const auditTraceSummary =
    document.getElementById("auditTraceSummary");

  const auditTimeline =
    document.getElementById("auditTimeline");

  const refreshAuditButton =
    document.getElementById("refreshAuditButton");

  const clarificationPanel =
          document.getElementById(
                  "clarificationPanel"
          );

  const clarificationQuestions =
          document.getElementById(
                  "clarificationQuestions"
          );

          const preferredMerchantsInput =
              document.getElementById(
                  "preferredMerchantsInput"
              );

          const avoidedMerchantsInput =
              document.getElementById(
                  "avoidedMerchantsInput"
              );

          const minimumReviewScoreInput =
              document.getElementById(
                  "minimumReviewScoreInput"
              );

          const minimumFulfillmentScoreInput =
              document.getElementById(
                  "minimumFulfillmentScoreInput"
              );

          const askBeforeNewMerchantInput =
              document.getElementById(
                  "askBeforeNewMerchantInput"
              );

          const savePreferencesButton =
              document.getElementById(
                  "savePreferencesButton"
              );

          const preferencesStatus =
              document.getElementById(
                  "preferencesStatus"
              );

const connectedAgentsList =
        document.getElementById(
                "connectedAgentsList"
        );
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

/**
 * Loads the current Consumer Preferences for the active principal
 * and populates the Command Center preference form.
 */
async function loadConsumerPreferences() {
  try {
    const response =
        await fetch(
            `/api/preferences/${PRINCIPAL_ID}`
        );

    if (!response.ok) {
      throw new Error(
          "Unable to load consumer preferences."
      );
    }

    const preferences =
        await response.json();

    preferredMerchantsInput.value =
        (
            preferences.preferredMerchants ||
            []
        ).join("\n");

    avoidedMerchantsInput.value =
        (
            preferences.avoidedMerchants ||
            []
        ).join("\n");

    minimumReviewScoreInput.value =
        preferences.preferredMinimumReviewScore ??
        "";

    minimumFulfillmentScoreInput.value =
        preferences.preferredMinimumFulfillmentScore ??
        "";

    askBeforeNewMerchantInput.checked =
        Boolean(
            preferences.askBeforeUsingNewMerchant
        );

  } catch (error) {
    console.error(
        "Failed to load Consumer Preferences.",
        error
    );

    preferencesStatus.textContent =
        "Unable to load preferences.";
  }
}

/**
 * Renders every active AI agent registered with Sentinq.
 *
 * Agent identity and provider metadata come from the backend
 * so the Connected Agents view always reflects the actual
 * runtime configuration rather than hardcoded UI content.
 */
function renderConnectedAgents(
        agents
) {
    if (!Array.isArray(agents) ||
            agents.length === 0) {

        connectedAgentsList.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">AI</div>
                <h3>No connected agents</h3>
                <p>
                    Sentinq does not currently have any active
                    AI agents registered.
                </p>
            </div>
        `;

        return;
    }

    connectedAgentsList.innerHTML =
            agents.map(agent => `
                <article class="panel agent-card">

                    <div class="agent-header">
                        <div class="agent-icon">
                            AI
                        </div>

                        <div>
                            <h3>
                                ${escapeHtml(
                                    agent.agentName
                                )}
                            </h3>

                            <p>
                                ${escapeHtml(
                                    formatProviderName(
                                        agent.provider
                                    )
                                )}
                                ·
                                ${escapeHtml(
                                    agent.model ||
                                    "Model not specified"
                                )}
                                ·
                                Shopping orchestration
                            </p>
                        </div>

                        <span class="badge badge-success">
                            Active
                        </span>
                    </div>

                    <div class="permission-grid">

                        <div>
                            <h4>Permitted</h4>

                            <ul class="permission-list allowed">
                                <li>
                                    Interpret shopping goals
                                </li>
                                <li>
                                    Read scoped consumer preferences
                                </li>
                                <li>
                                    Search merchant offers
                                </li>
                                <li>
                                    Request late-binding resolution
                                </li>
                                <li>
                                    Build candidate carts
                                </li>
                            </ul>
                        </div>

                        <div>
                            <h4>Restricted</h4>

                            <ul class="permission-list denied">
                                <li>
                                    Read raw payment credentials
                                </li>
                                <li>
                                    Execute without approval
                                </li>
                                <li>
                                    Change consumer preferences
                                </li>
                                <li>
                                    Exceed mandate authority
                                </li>
                            </ul>
                        </div>

                    </div>

                </article>
            `).join("");
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
 * 4. Handle clarification requests when the goal is incomplete.
 * 5. Store the returned mandate and audit information in browser state.
 * 6. Render the returned mandate, candidate carts, and execution decision.
 * 7. Refresh the execution history.
 */
async function handleRunOrchestration() {
  const formInput = readShoppingForm();

  if (!formInput.isValid) {
    elements.runStatus.textContent =
      formInput.validationMessage;

    return;
  }

  /*
   * Clear any clarification state left over from
   * a previous orchestration attempt.
   */
  clarificationPanel.classList.add("hidden");
  clarificationQuestions.innerHTML = "";

  const requestBody =
    buildOrchestrationRequest(formInput);

  setOrchestrationLoadingState(true);
  setWorkflowProgress(2);

  try {
    const result =
      await callShoppingOrchestration(
        requestBody
      );

    /*
     * Clarification is a valid orchestration state.
     * Stop the normal shopping workflow and ask the
     * consumer for the additional information Sentinq needs.
     */
    if (
      result.status ===
      "CLARIFICATION_REQUIRED"
    ) {
      renderClarificationRequest(
        result.questions
      );

      elements.runStatus.textContent =
        "Additional information required.";

      setWorkflowProgress(2);

      return;
    }

    /*
     * Store and render the successfully completed
     * shopping orchestration.
     */
    recordOrchestrationResult(
      result,
      formInput.goalText
    );

    renderShoppingResult(result);
    clarificationPanel.classList.add(
            "hidden"
    );

    /*
     * Refresh the execution history so the completed
     * orchestration immediately appears in the Audit view.
     */
    await loadExecutionTraces();

    elements.shopResults.classList.remove(
      "hidden"
    );

    elements.runStatus.textContent =
      "Orchestration complete.";

    setWorkflowProgress(6);

  } catch (error) {
    console.error(
      "Shopping orchestration failed",
      error
    );

    elements.runStatus.textContent =
      `Request failed: ${error.message}`;

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
 * - LateBindingResolutionService
 *
 * Clarification-required responses are treated as a valid workflow
 * state rather than a technical failure so the consumer can provide
 * the missing information and continue.
 */
async function callShoppingOrchestration(
        requestBody
) {
  const response =
      await fetch(
          "/api/shopping/orchestrate",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json"
            },
            body: JSON.stringify(
                requestBody
            )
          }
      );

  const responseBody =
      await response.json();

  /*
   * Clarification is a valid Sentinq workflow state.
   * Return the questions to the UI instead of throwing an error.
   */
  if (
      response.status === 422 &&
      responseBody.status ===
      "CLARIFICATION_REQUIRED"
  ) {
    return responseBody;
  }

  /*
   * Any other non-success response represents an
   * actual orchestration or platform failure.
   */
  if (!response.ok) {
    throw new Error(
        responseBody.message ||
        "Sentinq could not complete the request."
    );
  }

  return responseBody;
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

/**
 * Loads every AI agent currently registered with Sentinq.
 *
 * The returned agent identities are used by both the
 * Shopping Agent selector and the Connected AI Agents
 * dashboard to ensure both views remain synchronized.
 */
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

    populateAgentDropdown(
      agents
    );

    renderConnectedAgents(
      agents
    );

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

    connectedAgentsList.innerHTML = `
      <div class="empty-state">
        <div class="empty-icon">AI</div>
        <h3>Unable to load connected agents</h3>
        <p>
          Sentinq could not retrieve the registered
          AI agents.
        </p>
      </div>
    `;
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

/**
 * Persists the Consumer Preferences entered through the
 * Command Center so future orchestrations use the latest
 * consumer-defined merchant and fulfillment context.
 */
async function saveConsumerPreferences() {

  const preferences = {
    preferredMerchants:
        parseMerchantList(
            preferredMerchantsInput.value
        ),

    avoidedMerchants:
        parseMerchantList(
            avoidedMerchantsInput.value
        ),

    preferredMinimumReviewScore:
        parseOptionalNumber(
            minimumReviewScoreInput.value
        ),

    preferredMinimumFulfillmentScore:
        parseOptionalNumber(
            minimumFulfillmentScoreInput.value
        ),

    askBeforeUsingNewMerchant:
        askBeforeNewMerchantInput.checked
  };

  try {
    preferencesStatus.textContent =
        "Saving preferences...";

    const response =
        await fetch(
            `/api/preferences/${PRINCIPAL_ID}`,
            {
              method: "PUT",
              headers: {
                "Content-Type":
                    "application/json"
              },
              body:
                  JSON.stringify(
                      preferences
                  )
            }
        );

    if (!response.ok) {
      throw new Error(
          "Unable to save consumer preferences."
      );
    }

    preferencesStatus.textContent =
        "Consumer preferences saved.";

  } catch (error) {
    console.error(
        "Failed to save Consumer Preferences.",
        error
    );

    preferencesStatus.textContent =
        "Unable to save preferences.";
  }
}
// -----------------------------------------------------------------------------
// 7. Command Center rendering
// -----------------------------------------------------------------------------
/**
 * Converts a multiline merchant input into a normalized
 * merchant-name list used by Sentinq.
 */
function parseMerchantList(
    value
) {
  if (!value) {
    return [];
  }

  return value
      .split("\n")
      .map(merchant =>
          merchant.trim()
      )
      .filter(merchant =>
          merchant.length > 0
      );
}


/**
 * Converts an optional numeric form value into a number while
 * preserving null when the consumer has not set a threshold.
 */
function parseOptionalNumber(
    value
) {
  if (value === null ||
      value === undefined ||
      value.trim() === "") {
    return null;
  }

  return Number(value);
}

savePreferencesButton.addEventListener(
    "click",
    saveConsumerPreferences
);
loadConsumerPreferences();

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

/**
 * Builds the Command Center HTML for a synthesized Mandate Envelope.
 *
 * The mandate summary includes the consumer goal, hard constraints,
 * and the merchant-governance context that was active when the
 * mandate was created.
 */
function createMandateListItemHtml(
  mandate
) {
  return `
    <article class="mandate-row">

      <div class="mandate-row-top">
        <div>
          <h3>
            ${escapeHtml(
              mandate.objective
            )}
          </h3>

          <p>
            ${escapeHtml(
              mandate.originalGoal
            )}
          </p>
        </div>

        <span class="badge badge-accent">
          Synthesized
        </span>
      </div>

      <div class="mandate-meta">
        <span>
          ${formatMoney(
            mandate.maximumTotalCents
          )}
          maximum
        </span>

        <span>
          Delivery by
          ${formatDate(
            mandate.deliveryDeadline
          )}
        </span>
      </div>

      ${renderMandateMerchantPreferences(
        mandate
      )}

    </article>
  `;
}

/**
 * Renders the merchant-governance context captured inside
 * a Mandate Envelope.
 *
 * The values shown here reflect the preferences that were
 * active when Sentinq synthesized the mandate.
 */
function renderMandateMerchantPreferences(
  mandate
) {
  const preferredMerchants =
    mandate.preferredMerchants || [];

  const prohibitedMerchants =
    mandate.prohibitedMerchants || [];

  const minimumReviewScore =
    mandate.preferredMinimumReviewScore;

  const minimumFulfillmentScore =
    mandate.preferredMinimumFulfillmentScore;

  return `
    <div class="mandate-preferences">

      <div class="mandate-preference-row">
        <span>
          Preferred merchants
        </span>

        <strong>
          ${
            preferredMerchants.length > 0
              ? preferredMerchants
                  .map(escapeHtml)
                  .join(", ")
              : "None configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          Avoided merchants
        </span>

        <strong>
          ${
            prohibitedMerchants.length > 0
              ? prohibitedMerchants
                  .map(escapeHtml)
                  .join(", ")
              : "None configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          Minimum review score
        </span>

        <strong>
          ${
            minimumReviewScore ??
            "Not configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          Minimum fulfillment score
        </span>

        <strong>
          ${
            minimumFulfillmentScore ??
            "Not configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          New merchants
        </span>

        <strong>
          ${
            mandate.askBeforeUsingNewMerchant
              ? "Ask first"
              : "Allowed"
          }
        </strong>
      </div>

    </div>
  `;
}

/**
 * Renders the merchant-governance context captured inside
 * a Mandate Envelope.
 *
 * The values shown here reflect the preferences that were
 * active when Sentinq synthesized the mandate.
 */
function renderMandateMerchantPreferences(
  mandate
) {
  const preferredMerchants =
    mandate.preferredMerchants || [];

  const prohibitedMerchants =
    mandate.prohibitedMerchants || [];

  const minimumReviewScore =
    mandate.preferredMinimumReviewScore;

  const minimumFulfillmentScore =
    mandate.preferredMinimumFulfillmentScore;

  return `
    <div class="mandate-preferences">

      <div class="mandate-preference-row">
        <span>
          Preferred merchants
        </span>

        <strong>
          ${
            preferredMerchants.length > 0
              ? preferredMerchants
                  .map(escapeHtml)
                  .join(", ")
              : "None configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          Avoided merchants
        </span>

        <strong>
          ${
            prohibitedMerchants.length > 0
              ? prohibitedMerchants
                  .map(escapeHtml)
                  .join(", ")
              : "None configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          Minimum review score
        </span>

        <strong>
          ${
            minimumReviewScore ??
            "Not configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          Minimum fulfillment score
        </span>

        <strong>
          ${
            minimumFulfillmentScore ??
            "Not configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          New merchants
        </span>

        <strong>
          ${
            mandate.askBeforeUsingNewMerchant
              ? "Ask first"
              : "Allowed"
          }
        </strong>
      </div>

    </div>
  `;
}

/**
 * Renders the merchant-governance context captured inside
 * a Mandate Envelope.
 *
 * The values shown here reflect the preferences that were
 * active when Sentinq synthesized the mandate.
 */
function renderMandateMerchantPreferences(
  mandate
) {
  const preferredMerchants =
    mandate.preferredMerchants || [];

  const prohibitedMerchants =
    mandate.prohibitedMerchants || [];

  const minimumReviewScore =
    mandate.preferredMinimumReviewScore;

  const minimumFulfillmentScore =
    mandate.preferredMinimumFulfillmentScore;

  return `
    <div class="mandate-preferences">

      <div class="mandate-preference-row">
        <span>
          Preferred merchants
        </span>

        <strong>
          ${
            preferredMerchants.length > 0
              ? preferredMerchants
                  .map(escapeHtml)
                  .join(", ")
              : "None configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          Avoided merchants
        </span>

        <strong>
          ${
            prohibitedMerchants.length > 0
              ? prohibitedMerchants
                  .map(escapeHtml)
                  .join(", ")
              : "None configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          Minimum review score
        </span>

        <strong>
          ${
            minimumReviewScore ??
            "Not configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          Minimum fulfillment score
        </span>

        <strong>
          ${
            minimumFulfillmentScore ??
            "Not configured"
          }
        </strong>
      </div>

      <div class="mandate-preference-row">
        <span>
          New merchants
        </span>

        <strong>
          ${
            mandate.askBeforeUsingNewMerchant
              ? "Ask first"
              : "Allowed"
          }
        </strong>
      </div>

    </div>
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

/**
 * Displays the clarification questions returned
 * by Sentinq when additional consumer input is
 * required before a mandate can be created.
 */
function renderClarificationRequest(
        questions
) {

    clarificationQuestions.innerHTML =
            questions.map(question => `
                <div class="clarification-question">

                    <span>•</span>

                    <p>
                        ${escapeHtml(question)}
                    </p>

                </div>
            `).join("");

    clarificationPanel.classList.remove(
            "hidden"
    );
}
/**
 * Holds every execution trace retrieved from the Sentinq backend.
 *
 * Each trace represents one complete shopping orchestration,
 * including every interaction, provider call, governance
 * decision, and execution outcome.
 */
let executionTraces = [];

/**
 * Tracks the execution trace currently being viewed in the
 * Command Center.
 *
 * When the user selects another orchestration, this value
 * determines which trace is rendered in the detail panel.
 */
let selectedTraceId = null;

/**
 * Retrieves every execution trace from the Sentinq backend.
 *
 * After the traces are loaded, the execution history list is
 * refreshed and the currently selected trace (or the newest
 * trace) is displayed in the Execution Explorer.
 */

 /**
  * Builds a consumer-friendly summary of the Consumer Preferences
  * that Sentinq applied during an orchestration.
  *
  * This makes the governed context visible in the Execution Trace
  * without requiring the user to inspect raw JSON.
  */
 function renderPreferencesApplied(
         preferences
 ) {
     if (!preferences) {
         return "";
     }

     const preferredMerchants =
             preferences.preferredMerchants || [];

     const avoidedMerchants =
             preferences.avoidedMerchants || [];

     return `
         <div class="audit-governance-context">

             <div class="audit-context-group">
                 <span class="audit-context-label">
                     Preferred merchants
                 </span>

                 <strong>
                     ${
                         preferredMerchants.length > 0
                             ? preferredMerchants
                                 .map(escapeHtml)
                                 .join(", ")
                             : "None configured"
                     }
                 </strong>
             </div>

             <div class="audit-context-group">
                 <span class="audit-context-label">
                     Avoided merchants
                 </span>

                 <strong>
                     ${
                         avoidedMerchants.length > 0
                             ? avoidedMerchants
                                 .map(escapeHtml)
                                 .join(", ")
                             : "None configured"
                     }
                 </strong>
             </div>

             <div class="audit-context-group">
                 <span class="audit-context-label">
                     Minimum review score
                 </span>

                 <strong>
                     ${
                         preferences.preferredMinimumReviewScore ??
                         "Not configured"
                     }
                 </strong>
             </div>

             <div class="audit-context-group">
                 <span class="audit-context-label">
                     Minimum fulfillment score
                 </span>

                 <strong>
                     ${
                         preferences.preferredMinimumFulfillmentScore ??
                         "Not configured"
                     }
                 </strong>
             </div>

             <div class="audit-context-group">
                 <span class="audit-context-label">
                     New merchants
                 </span>

                 <strong>
                     ${
                         preferences.askBeforeUsingNewMerchant
                             ? "Ask before using"
                             : "Allowed"
                     }
                 </strong>
             </div>

         </div>
     `;
 }

async function loadExecutionTraces() {
  try {
    const response =
      await fetch("/api/audit/traces");

    if (!response.ok) {
      throw new Error(
        `Unable to load execution traces: ${response.status}`
      );
    }

    executionTraces =
      await response.json();

    renderExecutionTraceList(
      executionTraces
    );

    if (executionTraces.length === 0) {
      showEmptyTraceDetail();
      return;
    }

    const selectedTrace =
      executionTraces.find(
        trace =>
          trace.traceId === selectedTraceId
      ) || executionTraces[0];

    renderExecutionTraceDetail(
      selectedTrace
    );
  } catch (error) {
    console.error(
      "Failed to load execution traces.",
      error
    );

    auditTraceList.innerHTML = `
      <div class="audit-empty-state">
        <strong>Unable to load traces</strong>
        <p>
          Sentinq could not retrieve the execution history.
        </p>
      </div>
    `;
  }
}


/**
 * Renders every recorded orchestration as a compact, selectable
 * execution-history row.
 *
 * The history list is intentionally dense so that the selected
 * trace remains the primary focus of the Execution Explorer.
 */
function renderExecutionTraceList(
        traces
) {
    auditTraceCount.textContent =
            `${traces.length}`;

    if (!Array.isArray(traces) ||
            traces.length === 0) {

        auditTraceList.innerHTML = `
            <div class="audit-empty-state">
                <strong>No execution traces yet</strong>
                <p>
                    Run the Shopping Agent to create the first trace.
                </p>
            </div>
        `;

        return;
    }

    auditTraceList.innerHTML =
            traces.map(trace => {
                const isSelected =
                        trace.traceId === selectedTraceId;

                const provider =
                        formatProviderName(
                                trace.provider
                        );

                const model =
                        trace.model || "Unknown model";

                const timestamp =
                        formatTraceDate(
                                trace.startedAt
                        );

                const interactionCount =
                        trace.events?.length || 0;

                return `
                    <button
                            type="button"
                            class="audit-trace-row ${
                                    isSelected
                                            ? "active"
                                            : ""
                            }"
                            data-trace-id="${trace.traceId}"
                    >
                        <span
                                class="audit-trace-status-dot ${
                                        trace.completedAt
                                            ? "complete"
                                            : "in-progress"
                                }"
                                aria-hidden="true"
                        ></span>

                        <span class="audit-trace-main">
                            <strong>
                                ${provider}
                            </strong>

                            <span>
                                ${model}
                            </span>
                        </span>

                        <span class="audit-trace-meta">
                            <time>
                                ${timestamp}
                            </time>

                            <span>
                                ${interactionCount}
                                ${
                                    interactionCount === 1
                                        ? "interaction"
                                        : "interactions"
                                }
                            </span>
                        </span>
                    </button>
                `;
            }).join("");

    auditTraceList
            .querySelectorAll(
                    "[data-trace-id]"
            )
            .forEach(button => {
                button.addEventListener(
                        "click",
                        () => {
                            selectedTraceId =
                                    button.dataset.traceId;

                            const selectedTrace =
                                    executionTraces.find(
                                            trace =>
                                                    trace.traceId ===
                                                    selectedTraceId
                                    );

                            renderExecutionTraceList(
                                    executionTraces
                            );

                            renderExecutionTraceDetail(
                                    selectedTrace
                            );
                        }
                );
            });
}

/**
 * Displays the details for a single execution trace.
 *
 * The selected orchestration becomes the active trace in the
 * Execution Explorer, allowing the user to inspect the
 * execution summary and every interaction recorded during
 * the workflow.
 */
function renderExecutionTraceDetail(
  trace
) {
  if (!trace) {
    showEmptyTraceDetail();
    return;
  }

  selectedTraceId =
    trace.traceId;

  auditTraceEmpty.classList.add(
    "hidden"
  );

  auditTraceDetail.classList.remove(
    "hidden"
  );

  const provider =
    formatProviderName(
      trace.provider
    );

  auditTraceTitle.textContent =
    `${provider} shopping orchestration`;

  auditTraceSubtitle.textContent =
    `Trace ${trace.traceId}`;

  auditTraceStatus.textContent =
    trace.completedAt
      ? "Completed"
      : "In progress";

  auditTraceStatus.className =
    trace.completedAt
      ? "badge badge-success"
      : "badge";

  renderTraceSummary(trace);
  renderTraceEvents(trace.events || []);
}

/**
 * Builds the execution summary displayed at the top of the
 * selected trace.
 *
 * The summary provides a high-level view of the orchestration,
 * including the reasoning provider, model, execution time,
 * and the number of interactions recorded.
 */
function renderTraceSummary(
  trace
) {
  auditTraceSummary.innerHTML = `
    <div class="summary-item">
      <span>Provider</span>
      <strong>
        ${formatProviderName(trace.provider)}
      </strong>
    </div>

    <div class="summary-item">
      <span>Model</span>
      <strong>
        ${trace.model || "Unknown"}
      </strong>
    </div>

    <div class="summary-item">
      <span>Started</span>
      <strong>
        ${formatTraceDate(trace.startedAt)}
      </strong>
    </div>

    <div class="summary-item">
      <span>Interactions</span>
      <strong>
        ${trace.events?.length || 0}
      </strong>
    </div>
  `;
}

/**
 * Renders the complete execution timeline for the selected
 * orchestration.
 *
 * Each event represents a significant interaction performed
 * by Sentinq, allowing users to follow the execution flow
 * from request initiation through final decision.
 */
function renderTraceEvents(
  events
) {
  if (!Array.isArray(events) ||
      events.length === 0) {
    auditTimeline.innerHTML = `
      <div class="audit-empty-state">
        <strong>No interactions recorded</strong>
      </div>
    `;

    return;
  }

  auditTimeline.innerHTML =
    events.map(event => `
      <div class="audit-event">
        <span class="audit-dot"></span>

        <div class="audit-event-content">
          <div class="audit-event-heading">
            <strong>
              ${getAuditEventTitle(
                event.eventType
              )}
            </strong>

            <time>
              ${formatEventTime(
                event.timestamp
              )}
            </time>
          </div>

          <p>
            ${event.summary || ""}
          </p>

          ${
              event.eventType === "PREFERENCES_LOADED"
                  ? renderPreferencesApplied(
                      event.details
                  )
                  : ""
          }

          <div class="audit-event-meta">
            <span>
              ${formatComponentName(
                event.component
              )}
            </span>
          </div>

          ${
            event.details
              ? `
                <details class="audit-event-details">
                  <summary>
                    View interaction data
                  </summary>

                  <pre>${escapeHtml(
                      JSON.stringify(
                          sanitizeAuditDetails(
                              event.details
                          ),
                          null,
                          2
                      )
                  )}</pre>
                </details>
              `
              : ""
          }
        </div>
      </div>
    `).join("");
}


/**
 * Converts internal audit event identifiers into
 * consumer-friendly descriptions.
 *
 * Internal event names are intentionally decoupled from
 * presentation text so that the audit experience remains
 * understandable without exposing implementation details.
 */
function getAuditEventTitle(
  eventType
) {
  const titles = {
    REQUEST_RECEIVED:
      "Shopping request received",

    PRINCIPAL_LOADED:
      "Principal identity confirmed",

    AGENT_SELECTED:
      "Reasoning agent selected",

    DELEGATION_VALIDATED:
      "Shopping authority validated",

    PREFERENCES_LOADED:
      "Consumer preferences applied",

    GOAL_INTERPRETED:
      "Shopping goal interpreted",

    MANDATE_CREATED:
      "Mandate Envelope created",

    SEARCH_REQUEST_SENT:
      "Merchant search initiated",

    CANDIDATES_RECEIVED:
      "Merchant candidates received",

    CANDIDATES_RESOLVED:
      "Candidates evaluated",

    FINAL_DECISION:
      "Final decision produced",

    ORCHESTRATION_FAILED:
      "Orchestration failed"
  };

  return titles[eventType] ||
    formatComponentName(eventType);
}

function formatProviderName(
  provider
) {
  const names = {
    openai: "OpenAI",
    claude: "Claude",
    gemini: "Gemini"
  };

  return names[
    provider?.toLowerCase()
  ] || provider || "Unknown provider";
}

function formatComponentName(
  value
) {
  if (!value) {
    return "";
  }

  return value
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replaceAll("_", " ")
    .trim();
}

/**
 * Removes internal identifier fields from audit data before
 * displaying it in the consumer-facing Execution Explorer.
 *
 * Sentinq retains identifiers internally for orchestration and
 * traceability, but the normal Command Center view displays
 * meaningful business information rather than implementation IDs.
 */
function sanitizeAuditDetails(
        value
) {
    if (Array.isArray(value)) {
        return value.map(
                item =>
                        sanitizeAuditDetails(item)
        );
    }

    if (
        value !== null &&
        typeof value === "object"
    ) {
        return Object.fromEntries(
                Object.entries(value)
                        .filter(
                                ([key]) =>
                                        !key
                                                .toLowerCase()
                                                .endsWith("id")
                        )
                        .map(
                                ([key, nestedValue]) => [
                                    key,
                                    sanitizeAuditDetails(
                                            nestedValue
                                    )
                                ]
                        )
        );
    }

    return value;
}
/**
 * Converts an execution timestamp into a compact,
 * human-friendly value for the trace-history list.
 *
 * Runs from today and yesterday are labeled relative to the
 * current date; older executions display a short date.
 */
function formatTraceDate(
        timestamp
) {
    if (!timestamp) {
        return "Unknown";
    }

    const traceDate =
            new Date(timestamp);

    const now =
            new Date();

    const traceDay =
            new Date(
                    traceDate.getFullYear(),
                    traceDate.getMonth(),
                    traceDate.getDate()
            );

    const today =
            new Date(
                    now.getFullYear(),
                    now.getMonth(),
                    now.getDate()
            );

    const dayDifference =
            Math.round(
                    (
                            today.getTime() -
                            traceDay.getTime()
                    ) /
                    86400000
            );

    const time =
            traceDate.toLocaleTimeString(
                    [],
                    {
                        hour: "numeric",
                        minute: "2-digit"
                    }
            );

    if (dayDifference === 0) {
        return `Today · ${time}`;
    }

    if (dayDifference === 1) {
        return `Yesterday · ${time}`;
    }

    return traceDate.toLocaleDateString(
            [],
            {
                month: "short",
                day: "numeric",
                year:
                        traceDate.getFullYear() !==
                        now.getFullYear()
                                ? "numeric"
                                : undefined
            }
    );
}
/**
 * Converts an audit-event timestamp into a readable time
 * for the execution timeline.
 *
 * Events within a trace occur during the same orchestration,
 * so the timeline displays the precise time—including
 * seconds—without repeating the full calendar date.
 */
function formatEventTime(
        timestamp
) {
    if (!timestamp) {
        return "";
    }

    return new Date(
            timestamp
    ).toLocaleTimeString(
            [],
            {
                hour: "numeric",
                minute: "2-digit",
                second: "2-digit"
            }
    );
}

function escapeHtml(
  value
) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function showEmptyTraceDetail() {
  selectedTraceId = null;

  auditTraceEmpty.classList.remove(
    "hidden"
  );

  auditTraceDetail.classList.add(
    "hidden"
  );
}

refreshAuditButton.addEventListener(
  "click",
  loadExecutionTraces
);




// -----------------------------------------------------------------------------
// 8. Shopping result rendering
// -----------------------------------------------------------------------------

function renderShoppingResult(result) {
  renderMandate(result.mandate);

  renderSelectedCandidate(
    result.selectedCandidate
  );

  renderCandidateCarts(
    result.candidates ?? [],
    result.trustAssessedCandidates ?? []
  );
}

function createBlockedDecisionHtml() {
  return `
    <article class="panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">EXECUTION DECISION</p>
          <h3>No shortlisted candidate selected</h3>
        </div>
        <span class="badge badge-danger">Blocked</span>
      </div>

      <p class="section-copy">
        No preliminarily viable candidate was selected for this run.
      </p>
    </article>
  `;
}

function createSelectedCandidateHtml(
  selectedCandidate
) {
  const { offer, resolution } =
    selectedCandidate;

  return `
    <article class="panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">SELECTED CART</p>
          <h3>
            ${escapeHtml(
              offer.merchantName
            )}
          </h3>
        </div>

        <span class="badge badge-success">
          Shortlisted
        </span>
      </div>

      <p class="section-copy">
        ${escapeHtml(
          offer.productName
        )}
        ·
        ${formatMoney(
          resolution.resolvedTotalCents
        )}
        · delivery
        ${formatDate(
          resolution.estimatedDeliveryDate
        )}
      </p>
    </article>
  `;
}

function renderTrustMaps(
    trustAssessedCandidates
) {
    if (!trustAssessedCandidates?.length) {
        elements.trustMapResults.innerHTML = "";
        return;
    }

    elements.trustMapResults.innerHTML = `
        <div class="section-heading">
            <div>
                <p class="eyebrow">TRUST MAPS</p>
                <h2>Merchant trust evidence</h2>
            </div>
        </div>

        <div class="candidate-grid">
            ${trustAssessedCandidates
                .map(createTrustMapCandidateHtml)
                .join("")}
        </div>
    `;
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

function createCandidateCardHtml(
  candidate,
  trustAssessment
) {
  const { offer, resolution } = candidate;

  const statusClass =
    resolution.executable
      ? "badge-success"
      : "badge-danger";

  const statusLabel =
    resolution.executable
      ? "Executable"
      : "Blocked";

  return `
    <article class="candidate-card">

      <div class="candidate-header">
        <div>
          <h3>
            ${escapeHtml(offer.merchantName)}
          </h3>

          <p>
            ${escapeHtml(offer.productName)}
          </p>
        </div>

        <span class="badge ${statusClass}">
          ${statusLabel}
        </span>
      </div>

      <div class="price-grid">
        <span>Product</span>
        <strong>
          ${formatMoney(
            resolution.productPriceCents
          )}
        </strong>

        <span>Shipping</span>
        <strong>
          ${formatMoney(
            resolution.shippingCents
          )}
        </strong>

        <span>Tax</span>
        <strong>
          ${formatMoney(
            resolution.taxCents
          )}
        </strong>

        <span>Resolved total</span>
        <strong>
          ${formatMoney(
            resolution.resolvedTotalCents
          )}
        </strong>
      </div>

      ${
        createTrustMapSummaryHtml(
          trustAssessment
        )
      }

      <p class="section-copy">
        Delivery
        ${formatDate(
          resolution.estimatedDeliveryDate
        )}
      </p>

      ${
        renderMessages(
          "Violations",
          resolution.violations,
          "danger"
        )
      }

      ${
        renderMessages(
          "Warnings",
          resolution.warnings,
          "warning"
        )
      }

    </article>
  `;
}

function createTrustMapCandidateHtml(
    trustAssessedCandidate
) {
    const candidate =
        trustAssessedCandidate.candidate;

    const trustAssessment =
        trustAssessedCandidate.trustAssessment;

    const assessments =
        trustAssessment?.evidenceAssessments ?? [];

    return `
        <article class="candidate-card">

            <div class="candidate-header">
                <div>
                    <h3>
                        ${escapeHtml(candidate.merchantName)}
                    </h3>

                    <p>
                        ${escapeHtml(candidate.productName)}
                    </p>
                </div>

                <span class="badge badge-accent">
                    ${assessments.length} signals
                </span>
            </div>

            <div class="trust-evidence-list">

                ${assessments
                    .map(createTrustEvidenceHtml)
                    .join("")}

            </div>

        </article>
    `;
}

function createTrustEvidenceHtml(
    assessment
) {
    const evidence =
        assessment.originalEvidence;

    const interpretation =
        assessment.interpretation;

    return `
        <div class="trust-evidence-item">

            <div class="trust-evidence-header">

                <strong>
                    ${escapeHtml(
                        formatTrustDimension(
                            evidence.proposedDimension
                        )
                    )}
                </strong>

                <span class="badge">
                    ${escapeHtml(
                        interpretation.signal
                    )}
                </span>

            </div>

            <p class="section-copy">
                ${escapeHtml(
                    interpretation.contextualMeaning
                    || interpretation.apparentMeaning
                    || evidence.rawClaim
                )}
            </p>

            <div class="trust-evidence-meta">
                <span>
                    ${escapeHtml(
                        evidence.source?.name
                        ?? "Unknown source"
                    )}
                </span>

                <span>
                    Confidence:
                    ${formatConfidence(
                        interpretation.confidence
                    )}
                </span>

                ${
                    assessment.researchRounds > 0
                        ? `<span>
                               Researched:
                               ${assessment.researchRounds}
                               round
                           </span>`
                        : ""
                }
            </div>

        </div>
    `;
}

function formatTrustDimension(value) {
    if (!value) {
        return "Trust evidence";
    }

    return value
        .toLowerCase()
        .split("_")
        .map(word =>
            word.charAt(0).toUpperCase()
            + word.slice(1)
        )
        .join(" ");
}

function formatConfidence(value) {
    if (value === null ||
        value === undefined) {
        return "—";
    }

    return `${Math.round(value * 100)}%`;
}
function renderCandidateCarts(
  candidates,
  trustAssessedCandidates
) {
  const trustByOfferId =
    new Map(
      trustAssessedCandidates.map(item => [
        item.candidate?.offerId,
        item.trustAssessment
      ])
    );

  elements.candidateResults.innerHTML = `
    <div class="section-heading">
      <div>
        <p class="eyebrow">CANDIDATE ASSESSMENT</p>
        <h2>Candidate carts</h2>
      </div>
    </div>

    <div class="candidate-grid">
      ${
        candidates
          .map(candidate => {
            const trustAssessment =
              trustByOfferId.get(
                candidate.offer?.offerId
              );

            return createCandidateCardHtml(
              candidate,
              trustAssessment
            );
          })
          .join("")
      }
    </div>
  `;
}

function createCandidateCardHtml(
  candidate,
  trustAssessment
) {
  const { offer, resolution } = candidate;

  const statusClass =
    resolution.executable
      ? "badge-success"
      : "badge-danger";

  const statusLabel =
    resolution.executable
      ? "Executable"
      : "Blocked";

  return `
    <article class="candidate-card">

      <div class="candidate-header">
        <div>
          <h3>${escapeHtml(offer.merchantName)}</h3>
          <p>${escapeHtml(offer.productName)}</p>
        </div>

        <span class="badge ${statusClass}">
          ${statusLabel}
        </span>
      </div>

      <div class="price-grid">
        <span>Product</span>
        <strong>${formatMoney(resolution.productPriceCents)}</strong>

        <span>Shipping</span>
        <strong>${formatMoney(resolution.shippingCents)}</strong>

        <span>Tax</span>
        <strong>${formatMoney(resolution.taxCents)}</strong>

        <span>Resolved total</span>
        <strong>${formatMoney(resolution.resolvedTotalCents)}</strong>
      </div>

      ${createTrustMapSummaryHtml(trustAssessment)}

      <p class="section-copy">
        Delivery ${formatDate(resolution.estimatedDeliveryDate)}
      </p>

      ${renderMessages(
        "Violations",
        resolution.violations,
        "danger"
      )}

      ${renderMessages(
        "Warnings",
        resolution.warnings,
        "warning"
      )}

    </article>
  `;
}

function renderMessages(
  title,
  messages,
  type
) {
  if (!messages?.length) {
    return "";
  }

  return `
    <div class="message-group message-group-${type}">
      <strong>${escapeHtml(title)}</strong>

      <ul>
        ${
          messages
            .map(
              message => `
                <li>
                  ${escapeHtml(message)}
                </li>
              `
            )
            .join("")
        }
      </ul>
    </div>
  `;
}

function createTrustMapSummaryHtml(
  trustAssessment
) {
  if (!trustAssessment?.synthesis) {
    return `
      <div class="trust-map-summary">
        <p class="eyebrow">
          TRUST MAP
        </p>

        <p class="section-copy">
          No Trust Map assessment was run
          for this candidate.
        </p>
      </div>
    `;
  }

  const synthesis =
    trustAssessment.synthesis;

  const themes =
    synthesis.themes ?? [];

  const evidenceCount =
    (trustAssessment.observedEvidence?.length ?? 0)
    +
    (trustAssessment.researchedEvidence?.length ?? 0);

  const confidence =
    Math.round(
      (synthesis.confidence ?? 0) * 100
    );

  return `
    <div class="trust-map-summary">

      <div class="trust-map-heading">
        <div>
          <p class="eyebrow">
            TRUST MAP
          </p>

          <h4>
            My take
          </h4>
        </div>

        <span class="badge">
          ${confidence}% confidence
        </span>
      </div>

      <p class="trust-map-take">
        ${escapeHtml(
          buildTrustMapTake(themes)
        )}
      </p>

      <div class="trust-theme-list">
        ${
          themes
            .map(
              createTrustThemeHtml
            )
            .join("")
        }
      </div>

      <details class="trust-map-evidence">
        <summary>
          View evidence ·
          ${evidenceCount} sources
        </summary>

        ${
          createTrustEvidenceHtml(
            trustAssessment
          )
        }
      </details>

    </div>
  `;
}

function buildTrustMapTake(
  themes
) {
  if (!themes?.length) {
    return "I don't have enough evidence to form a useful view yet.";
  }

  const concerning =
    themes.filter(
      theme =>
        theme.signal === "CONCERNING" ||
        theme.signal === "STRONGLY_CONCERNING"
    );

  const mixed =
    themes.filter(
      theme =>
        theme.signal === "MIXED"
    );

  const supportive =
    themes.filter(
      theme =>
        theme.signal === "SUPPORTIVE" ||
        theme.signal === "STRONGLY_SUPPORTIVE"
    );

  if (concerning.length > 0) {
    return "I found meaningful concerns here. I would look closely at the flagged areas before relying on this merchant.";
  }

  if (mixed.length > 0 &&
      supportive.length > 0) {
    return "I see some reassuring evidence, but the picture isn't clean enough for an unqualified recommendation. There are a few areas I'd keep in mind.";
  }

  if (mixed.length > 0) {
    return "The evidence is mixed. I wouldn't treat this merchant as clearly good or clearly bad based on what I found.";
  }

  if (supportive.length > 0) {
    return "The evidence is generally reassuring. I didn't find a major trust issue in the areas I assessed.";
  }

  return "The evidence doesn't point strongly in either direction yet.";
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

function createTrustThemeHtml(
  theme
) {
  return `
    <div class="trust-theme">

      <div class="trust-theme-header">
        <strong>
          ${escapeHtml(
            formatTrustDimension(
              theme.dimension
            )
          )}
        </strong>

        <span class="badge">
          ${escapeHtml(
            formatTrustSignal(
              theme.signal
            )
          )}
        </span>
      </div>

      <p>
        ${escapeHtml(
          theme.theme
        )}
      </p>

    </div>
  `;
}

function formatTrustDimension(
  dimension
) {
  if (!dimension) {
    return "Trust";
  }

  return dimension
    .toLowerCase()
    .split("_")
    .map(
      word =>
        word.charAt(0).toUpperCase()
        + word.slice(1)
    )
    .join(" ");
}

function formatTrustSignal(
  signal
) {
  if (!signal) {
    return "Unknown";
  }

  return signal
    .toLowerCase()
    .split("_")
    .map(
      word =>
        word.charAt(0).toUpperCase()
        + word.slice(1)
    )
    .join(" ");
}

function createTrustEvidenceHtml(
  trustAssessment
) {
  const observed =
    trustAssessment.observedEvidence ?? [];

  const researched =
    trustAssessment.researchedEvidence ?? [];

  const allEvidence = [
    ...observed.map(item => ({
      ...item,
      evidenceType: "Observed"
    })),

    ...researched.map(item => ({
      ...item,
      evidenceType: "Researched"
    }))
  ];

  return `
    <div class="trust-evidence-list">
      ${
        allEvidence
          .map(
            evidence => `
              <div class="trust-evidence-item">

                <div class="trust-evidence-meta">
                  <strong>
                    ${escapeHtml(
                      evidence.source?.name
                      ?? "Unknown source"
                    )}
                  </strong>

                  <span>
                    ${escapeHtml(
                      evidence.evidenceType
                    )}
                  </span>
                </div>

                <p>
                  ${escapeHtml(
                    evidence.rawClaim ?? ""
                  )}
                </p>

                ${
                  evidence.sourceUrl
                    ? `
                      <a
                        href="${escapeHtml(
                          evidence.sourceUrl
                        )}"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        Open source
                      </a>
                    `
                    : ""
                }

              </div>
            `
          )
          .join("")
      }
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
