/**
 * Feature Flag Service - Single Page Dashboard Application Logic
 */

let currentProjects = [];
let currentProject = null;
let currentFlags = [];

document.addEventListener('DOMContentLoaded', () => {
  initDashboard();
});

async function initDashboard() {
  await fetchProjects();
}

async function fetchProjects() {
  try {
    const res = await fetch('/api/projects');
    if (!res.ok) throw new Error('Failed to fetch projects');
    currentProjects = await res.json();

    const select = document.getElementById('projectSelect');
    select.innerHTML = '';

    if (currentProjects.length === 0) {
      // Auto create a demo project if empty
      await autoCreateDefaultProject();
      return;
    }

    currentProjects.forEach(proj => {
      const opt = document.createElement('option');
      opt.value = proj.id;
      opt.textContent = proj.name;
      select.appendChild(opt);
    });

    select.onchange = (e) => onProjectChange(e.target.value);
    
    // Select first project by default
    if (!currentProject || !currentProjects.find(p => p.id === currentProject.id)) {
      onProjectChange(currentProjects[0].id);
    } else {
      onProjectChange(currentProject.id);
    }
  } catch (err) {
    console.error('Error initializing projects:', err);
  }
}

async function autoCreateDefaultProject() {
  try {
    const res = await fetch('/api/projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Default App Workspace' })
    });
    if (res.ok) {
      await fetchProjects();
    }
  } catch (err) {
    console.error('Failed to auto-create default project:', err);
  }
}

function onProjectChange(projectId) {
  currentProject = currentProjects.find(p => p.id === projectId);
  if (!currentProject) return;

  document.getElementById('projectSelect').value = currentProject.id;
  document.getElementById('apiKeyDisplay').textContent = currentProject.apiKey;
  document.getElementById('snippetKey').textContent = currentProject.apiKey;

  fetchFlags(currentProject.id);
}

async function fetchFlags(projectId) {
  try {
    const res = await fetch(`/api/flags?projectId=${projectId}`);
    if (!res.ok) throw new Error('Failed to fetch flags');
    currentFlags = await res.json();
    renderDashboard();
  } catch (err) {
    console.error('Error fetching flags:', err);
  }
}

function renderDashboard() {
  // Update header stats
  const totalFlags = currentFlags.length;
  const activeFlags = currentFlags.filter(f => f.isEnabled).length;
  const totalEvals = currentFlags.reduce((acc, f) => acc + (f.trueEvaluations || 0) + (f.falseEvaluations || 0), 0);

  document.getElementById('statTotalFlags').textContent = totalFlags;
  document.getElementById('statActiveFlags').textContent = activeFlags;
  document.getElementById('statEvaluations').textContent = totalEvals;

  // Render flag list
  const flagListEl = document.getElementById('flagList');
  const evalFlagSelect = document.getElementById('evalFlagSelect');

  evalFlagSelect.innerHTML = '<option value="">-- Select Flag --</option>';

  if (currentFlags.length === 0) {
    flagListEl.innerHTML = `
      <div style="text-align:center; padding:3rem; background:var(--bg-card); border-radius:12px; border:1px solid var(--border-color);">
        <p style="color:var(--text-muted); font-size:1.05rem;">No feature flags configured for this project yet.</p>
        <button class="btn btn-primary" style="margin-top:1rem;" onclick="openModal('createFlagModal')">+ Create First Flag</button>
      </div>
    `;
    return;
  }

  flagListEl.innerHTML = currentFlags.map(flag => {
    // Populate eval select option
    const opt = document.createElement('option');
    opt.value = flag.flagKey;
    opt.textContent = `${flag.name} (${flag.flagKey})`;
    evalFlagSelect.appendChild(opt);

    const isActive = flag.isEnabled;
    const trueCount = flag.trueEvaluations || 0;
    const falseCount = flag.falseEvaluations || 0;

    return `
      <div class="flag-card" id="flag-card-${flag.id}">
        <div class="flag-top-row">
          <div class="flag-header">
            <div class="flag-name">${escapeHtml(flag.name)}</div>
            <span class="flag-key">${escapeHtml(flag.flagKey)}</span>
          </div>

          <div class="toggle-wrapper">
            <span class="status-badge ${isActive ? 'badge-active' : 'badge-inactive'}">
              ${isActive ? 'Active' : 'Inactive'}
            </span>
            <label class="switch">
              <input type="checkbox" ${isActive ? 'checked' : ''} onchange="toggleFlagState('${flag.id}', this.checked)">
              <span class="slider"></span>
            </label>
            <button class="btn btn-danger" style="padding:0.3rem 0.6rem; font-size:0.8rem;" onclick="deleteFlag('${flag.id}')" title="Delete Flag">✕</button>
          </div>
        </div>

        <div class="flag-desc">${escapeHtml(flag.description || 'No description provided.')}</div>

        <!-- Rollout Slider -->
        <div class="rollout-control">
          <div class="rollout-header">
            <span>Rollout Target Population</span>
            <span class="rollout-pct-badge" id="pct-badge-${flag.id}">${flag.rolloutPercentage}%</span>
          </div>
          <input type="range" class="range-input" min="0" max="100" value="${flag.rolloutPercentage}"
            oninput="updateSliderBadge('${flag.id}', this.value)"
            onchange="updateFlagRollout('${flag.id}', this.value)">
        </div>

        <!-- Evaluation Stats Summary -->
        <div style="margin-top:0.75rem; font-size:0.8rem; color:var(--text-dim); display:flex; gap:1.5rem;">
          <span>Evaluations: <strong style="color:var(--text-muted);">${trueCount + falseCount}</strong></span>
          <span>True: <strong style="color:var(--success-color);">${trueCount}</strong></span>
          <span>False: <strong style="color:var(--danger-color);">${falseCount}</strong></span>
        </div>
      </div>
    `;
  }).join('');
}

function updateSliderBadge(flagId, val) {
  const badge = document.getElementById(`pct-badge-${flagId}`);
  if (badge) badge.textContent = `${val}%`;
}

async function toggleFlagState(flagId, isEnabled) {
  try {
    const res = await fetch(`/api/flags/${flagId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ isEnabled: isEnabled })
    });
    if (!res.ok) throw new Error('Failed to update flag state');
    await fetchFlags(currentProject.id);
  } catch (err) {
    console.error('Error toggling flag:', err);
  }
}

async function updateFlagRollout(flagId, rolloutPercentage) {
  try {
    const res = await fetch(`/api/flags/${flagId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rolloutPercentage: parseInt(rolloutPercentage, 10) })
    });
    if (!res.ok) throw new Error('Failed to update rollout percentage');
    await fetchFlags(currentProject.id);
  } catch (err) {
    console.error('Error updating rollout:', err);
  }
}

async function deleteFlag(flagId) {
  if (!confirm('Are you sure you want to delete this feature flag?')) return;
  try {
    const res = await fetch(`/api/flags/${flagId}`, { method: 'DELETE' });
    if (!res.ok) throw new Error('Failed to delete flag');
    await fetchFlags(currentProject.id);
  } catch (err) {
    console.error('Error deleting flag:', err);
  }
}

async function handleCreateProject(e) {
  e.preventDefault();
  const nameInput = document.getElementById('projectNameInput');
  const name = nameInput.value.trim();
  if (!name) return;

  try {
    const res = await fetch('/api/projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name })
    });
    if (!res.ok) throw new Error('Failed to create project');
    const created = await res.json();
    nameInput.value = '';
    closeModal('createProjectModal');
    await fetchProjects();
    onProjectChange(created.id);
  } catch (err) {
    alert('Failed to create project: ' + err.message);
  }
}

async function handleCreateFlag(e) {
  e.preventDefault();
  if (!currentProject) return;

  const flagKey = document.getElementById('flagKeyInput').value.trim();
  const name = document.getElementById('flagNameInput').value.trim();
  const description = document.getElementById('flagDescInput').value.trim();
  const rolloutPercentage = parseInt(document.getElementById('flagRolloutInput').value, 10);
  const isEnabled = document.getElementById('flagEnabledInput').checked;

  try {
    const res = await fetch('/api/flags', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        projectId: currentProject.id,
        flagKey,
        name,
        description,
        rolloutPercentage,
        isEnabled
      })
    });

    if (!res.ok) {
      const errData = await res.json();
      throw new Error(errData.message || 'Failed to create feature flag');
    }

    document.getElementById('createFlagForm').reset();
    document.getElementById('flagRolloutInput').value = 100;
    document.getElementById('flagEnabledInput').checked = true;
    closeModal('createFlagModal');

    await fetchFlags(currentProject.id);
  } catch (err) {
    alert('Error creating flag: ' + err.message);
  }
}

async function testEvaluation() {
  if (!currentProject) return alert('Please select a project first');
  const flagKeySelect = document.getElementById('evalFlagSelect');
  const flagKey = flagKeySelect.value;
  const userId = document.getElementById('evalUserIdInput').value.trim();

  if (!flagKey) return alert('Please select a flag to evaluate');
  if (!userId) return alert('Please enter a User ID');

  const startTime = performance.now();
  try {
    const res = await fetch(`/api/evaluate/${encodeURIComponent(flagKey)}?projectApiKey=${encodeURIComponent(currentProject.apiKey)}&userId=${encodeURIComponent(userId)}`);
    const duration = Math.round(performance.now() - startTime);

    if (!res.ok) {
      const errData = await res.json();
      throw new Error(errData.message || 'Evaluation API call failed');
    }

    const data = await res.json();
    const box = document.getElementById('evalResultBox');
    const statusText = document.getElementById('evalStatusText');
    const metaText = document.getElementById('evalMetaText');

    box.className = `eval-result-box ${data.enabled ? 'enabled' : 'disabled'}`;
    statusText.textContent = data.enabled ? '✔ FEATURE ENABLED' : '✖ FEATURE DISABLED';
    metaText.innerHTML = `Source: <strong>${data.source}</strong> (${duration}ms response time)`;

    // Refresh flag stats after evaluation
    fetchFlags(currentProject.id);
  } catch (err) {
    alert('Evaluation error: ' + err.message);
  }
}

function copyApiKey() {
  if (!currentProject || !currentProject.apiKey) return;
  navigator.clipboard.writeText(currentProject.apiKey).then(() => {
    alert('API Key copied to clipboard!');
  }).catch(() => {
    prompt('Copy API Key:', currentProject.apiKey);
  });
}

function openModal(id) {
  document.getElementById(id).classList.add('active');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('active');
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, function(m) {
    return {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;'
    }[m];
  });
}
