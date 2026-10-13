/**
 * Enterprise Knowledge Portal - Vanilla JavaScript Application
 * Handles authentication, role-based functionality access, document management,
 * semantic search, and the AI research agent workflow.
 */

// Application State
const AppState = {
    currentUser: null,
    activeSection: 'dashboardSection',
    accessibleDocuments: []
};

// DOM Ready Initialization
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initAuthPortal();
    initDashboard();
    initSearch();
    initAiAgent();
    initDocumentManagement();
    initFeedback();
    initAnalytics();
    initUsersDirectory();
    initModal();

    // Check if user has an existing active session, or display Auth Portal
    checkSessionOrShowAuth();
});

// ===================================================================
// Authentication & Role-Based Access Control
// ===================================================================
function initAuthPortal() {
    // Tab switching in Auth Gateway
    const authTabBtns = document.querySelectorAll('.auth-tab-btn');
    authTabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetViewId = btn.getAttribute('data-auth-view');
            switchAuthTab(targetViewId);
        });
    });

    // Quick links between forms
    const linkToRegister = document.getElementById('linkToRegister');
    if (linkToRegister) {
        linkToRegister.addEventListener('click', () => switchAuthTab('employeeRegisterView'));
    }

    const linkToEmpLogin = document.getElementById('linkToEmpLogin');
    if (linkToEmpLogin) {
        linkToEmpLogin.addEventListener('click', () => switchAuthTab('employeeLoginView'));
    }

    // Demo credentials auto-fill buttons
    const btnFillEmpDemo = document.getElementById('btnFillEmpDemo');
    if (btnFillEmpDemo) {
        btnFillEmpDemo.addEventListener('click', () => {
            const userIn = document.getElementById('empLoginUser');
            const passIn = document.getElementById('empLoginPass');
            if (userIn) userIn.value = 'employee1';
            if (passIn) passIn.value = 'password123';
        });
    }

    const btnFillAdminDemo = document.getElementById('btnFillAdminDemo');
    if (btnFillAdminDemo) {
        btnFillAdminDemo.addEventListener('click', () => {
            const userIn = document.getElementById('adminLoginUser');
            const passIn = document.getElementById('adminLoginPass');
            if (userIn) userIn.value = 'admin_user';
            if (passIn) passIn.value = 'password123';
        });
    }

    // 1. Employee Login Form
    const formEmpLogin = document.getElementById('formEmployeeLogin');
    if (formEmpLogin) {
        formEmpLogin.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearAuthAlert();

            const username = document.getElementById('empLoginUser').value.trim();
            const password = document.getElementById('empLoginPass').value;
            const submitBtn = document.getElementById('btnEmpLoginSubmit');

            if (!username || !password) {
                showAuthAlert('Please enter both username and password.');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.textContent = 'Verifying Credentials...';

            try {
                const res = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });

                submitBtn.disabled = false;
                submitBtn.textContent = 'Sign In as Employee';

                if (!res.ok) {
                    const err = await res.json().catch(() => ({}));
                    showAuthAlert(err.message || 'Invalid username or password. Please try again.');
                    return;
                }

                const user = await res.json();
                onLoginSuccess(user, true);

            } catch (err) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Sign In as Employee';
                console.error('Employee Login error:', err);
                showAuthAlert('Unable to communicate with the authentication server.');
            }
        });
    }

    // 2. Employee Registration Form
    const formEmpRegister = document.getElementById('formEmployeeRegister');
    if (formEmpRegister) {
        formEmpRegister.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearAuthAlert();

            const fullName = document.getElementById('regFullName').value.trim();
            const username = document.getElementById('regUsername').value.trim();
            const password = document.getElementById('regPassword').value;
            const confirmPassword = document.getElementById('regConfirmPassword').value;
            const submitBtn = document.getElementById('btnRegisterSubmit');

            if (password !== confirmPassword) {
                showAuthAlert('Passwords do not match. Please re-enter identical passwords.');
                return;
            }

            if (password.length < 4) {
                showAuthAlert('Password must be at least 4 characters long.');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.textContent = 'Creating Account...';

            try {
                const res = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ fullName, username, password })
                });

                submitBtn.disabled = false;
                submitBtn.textContent = 'Create Employee Account & Sign In';

                if (!res.ok) {
                    const err = await res.json().catch(() => ({}));
                    showAuthAlert(err.message || 'Registration failed. The username may already exist.');
                    return;
                }

                const user = await res.json();
                formEmpRegister.reset();
                onLoginSuccess(user, true);
                showAlert(`🎉 Account created! Welcome to the Knowledge Portal, ${user.fullName}.`, 'success');

            } catch (err) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Create Employee Account & Sign In';
                console.error('Registration error:', err);
                showAuthAlert('Registration request could not be processed.');
            }
        });
    }

    // 3. Administrator Login Form (Strict Admin Gateway)
    const formAdminLogin = document.getElementById('formAdminLogin');
    if (formAdminLogin) {
        formAdminLogin.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearAuthAlert();

            const username = document.getElementById('adminLoginUser').value.trim();
            const password = document.getElementById('adminLoginPass').value;
            const submitBtn = document.getElementById('btnAdminLoginSubmit');

            if (!username || !password) {
                showAuthAlert('Please enter administrator username and password.');
                return;
            }

            submitBtn.disabled = true;
            submitBtn.textContent = 'Authenticating Admin Security Clearance...';

            try {
                const res = await fetch('/api/auth/admin-login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });

                submitBtn.disabled = false;
                submitBtn.textContent = 'Sign In as Administrator';

                if (!res.ok) {
                    const err = await res.json().catch(() => ({}));
                    if (res.status === 403) {
                        showAuthAlert(`🛑 Access Denied: ${err.message || 'Account does not have administrator clearance.'}`);
                    } else {
                        showAuthAlert(err.message || 'Invalid administrator credentials.');
                    }
                    return;
                }

                const user = await res.json();
                onLoginSuccess(user, true);

            } catch (err) {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Sign In as Administrator';
                console.error('Admin Login error:', err);
                showAuthAlert('Admin gateway could not connect to security service.');
            }
        });
    }

    // Sign Out Action
    const btnLogout = document.getElementById('btnLogout');
    if (btnLogout) {
        btnLogout.addEventListener('click', async () => {
            try {
                await fetch('/api/auth/logout', { method: 'POST' });
            } catch (e) {}
            AppState.currentUser = null;
            showAuthScreen();
            showAlert('You have been securely signed out.', 'info');
        });
    }
}

function switchAuthTab(targetViewId) {
    clearAuthAlert();
    document.querySelectorAll('.auth-tab-btn').forEach(b => {
        if (b.getAttribute('data-auth-view') === targetViewId) {
            b.classList.add('active');
        } else {
            b.classList.remove('active');
        }
    });

    document.querySelectorAll('.auth-view').forEach(v => {
        if (v.id === targetViewId) {
            v.classList.add('active');
        } else {
            v.classList.remove('active');
        }
    });
}

function showAuthAlert(msg) {
    const alertBox = document.getElementById('authAlert');
    const alertText = document.getElementById('authAlertText');
    if (alertBox && alertText) {
        alertText.textContent = msg;
        alertBox.style.display = 'block';
    }
}

function clearAuthAlert() {
    const alertBox = document.getElementById('authAlert');
    if (alertBox) alertBox.style.display = 'none';
}

async function checkSessionOrShowAuth() {
    try {
        const res = await fetch('/api/auth/me');
        if (res.ok) {
            const user = await res.json();
            onLoginSuccess(user, false);
            return;
        }
    } catch (e) {}
    showAuthScreen();
}

function showAuthScreen() {
    const authScreen = document.getElementById('authScreen');
    const appContainer = document.getElementById('appContainer');
    if (authScreen) authScreen.style.display = 'flex';
    if (appContainer) appContainer.style.display = 'none';
    clearAuthAlert();
}

function showAppScreen() {
    const authScreen = document.getElementById('authScreen');
    const appContainer = document.getElementById('appContainer');
    if (authScreen) authScreen.style.display = 'none';
    if (appContainer) appContainer.style.display = 'block';
}

function onLoginSuccess(user, notify = true) {
    AppState.currentUser = user;
    applyUserPermissions(user);
    showAppScreen();

    if (notify) {
        const roleLabel = user.role === 'ROLE_ADMIN' ? 'Portal Administrator' : 'Staff Employee';
        showAlert(`Signed in as ${user.fullName} (${roleLabel}). Clearance active.`, 'success');
    }

    // Default to dashboard
    switchTab('dashboardSection');
    refreshActiveSection();
}

function applyUserPermissions(user) {
    if (!user) return;
    const isAdmin = user.role === 'ROLE_ADMIN';

    // 1. Header Profile Badge
    const nameEl = document.getElementById('userName');
    const roleBadgeEl = document.getElementById('userRoleBadge');
    const avatarEl = document.getElementById('userAvatar');
    const clearancePill = document.getElementById('userClearancePill');

    if (nameEl) nameEl.textContent = user.fullName;
    if (avatarEl) avatarEl.textContent = user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U';

    if (roleBadgeEl) {
        roleBadgeEl.textContent = isAdmin ? 'ADMINISTRATOR' : 'EMPLOYEE';
        roleBadgeEl.className = isAdmin ? 'badge badge-admin' : 'badge badge-employee';
    }

    if (clearancePill) {
        clearancePill.textContent = isAdmin
            ? 'Clearance: Tier 3 (Full Admin)'
            : 'Clearance: Tier 1 (Public + Employee)';
    }

    // 2. Navigation Tab Visibility & Titles
    const tabAnalytics = document.getElementById('tabAnalytics');
    if (tabAnalytics) {
        tabAnalytics.style.display = isAdmin ? 'inline-flex' : 'none';
    }

    const tabUsers = document.getElementById('tabUsers');
    if (tabUsers) {
        tabUsers.style.display = isAdmin ? 'inline-flex' : 'none';
    }

    const tabDocsLabel = document.getElementById('tabDocumentsLabel');
    if (tabDocsLabel) {
        tabDocsLabel.textContent = isAdmin ? 'Document Ingestion & Management' : 'Document Library';
    }

    // 3. Document Management Functionality Isolation
    const uploadCard = document.getElementById('documentUploadCard');
    const employeeNotice = document.getElementById('employeeReadonlyNotice');

    if (uploadCard) {
        uploadCard.style.display = isAdmin ? 'block' : 'none';
    }
    if (employeeNotice) {
        employeeNotice.style.display = isAdmin ? 'none' : 'block';
    }
}

// ===================================================================
// Navigation Tabs
// ===================================================================
function initNavigation() {
    const tabs = document.querySelectorAll('.nav-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const targetId = tab.getAttribute('data-target');
            switchTab(targetId);
        });
    });
}

function switchTab(sectionId) {
    // Guard admin-only sections
    if ((sectionId === 'analyticsSection' || sectionId === 'usersSection') && AppState.currentUser?.role !== 'ROLE_ADMIN') {
        showAlert('Access Denied: This section requires Portal Administrator security clearance.', 'danger');
        return;
    }

    document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));

    const activeTab = document.querySelector(`.nav-tab[data-target="${sectionId}"]`);
    const activeSection = document.getElementById(sectionId);

    if (activeTab) activeTab.classList.add('active');
    if (activeSection) activeSection.classList.add('active');

    AppState.activeSection = sectionId;
    refreshActiveSection();
}

function refreshActiveSection() {
    switch (AppState.activeSection) {
        case 'dashboardSection':
            loadDashboardData();
            break;
        case 'documentsSection':
            loadDocumentsTable();
            break;
        case 'feedbackSection':
            populateFeedbackDocOptions();
            break;
        case 'analyticsSection':
            loadAnalyticsData();
            break;
        case 'usersSection':
            loadUsersDirectory();
            break;
    }
}


// ===================================================================
// Notification Banner Helper
// ===================================================================
function showAlert(message, type = 'info') {
    const banner = document.getElementById('alertBanner');
    const textEl = document.getElementById('alertMessage');
    const closeBtn = document.getElementById('alertCloseBtn');

    if (!banner || !textEl) return;

    banner.className = `alert-banner alert-${type}`;
    textEl.textContent = message;
    banner.style.display = 'flex';

    closeBtn.onclick = () => {
        banner.style.display = 'none';
    };

    setTimeout(() => {
        if (banner.style.display !== 'none') {
            banner.style.display = 'none';
        }
    }, 5000);
}

// ===================================================================
// SCREEN A: DASHBOARD
// ===================================================================
function initDashboard() {
    loadDashboardData();
}

async function loadDashboardData() {
    try {
        const res = await fetch('/api/knowledge/analytics');
        if (!res.ok) return;
        const data = await res.json();

        // Update KPIs
        document.getElementById('kpiActiveDocs').textContent = data.totalActiveDocuments || 0;
        document.getElementById('kpiArchivedDocs').textContent = data.totalArchivedDocuments || 0;
        document.getElementById('kpiAwaitingReindex').textContent = data.totalAwaitingReindex || 0;
        document.getElementById('kpiAvgRating').textContent = (data.overallAverageRating || 0).toFixed(1);

        // Populate Category Table
        const catTbody = document.getElementById('dashboardCategoryTbody');
        if (catTbody) {
            if (!data.documentsByCategoryAndStatus || data.documentsByCategoryAndStatus.length === 0) {
                catTbody.innerHTML = '<tr><td colspan="3" class="text-center">No documents in knowledge base yet.</td></tr>';
            } else {
                catTbody.innerHTML = data.documentsByCategoryAndStatus.map(item => `
                    <tr>
                        <td><strong>${escapeHtml(item.category)}</strong></td>
                        <td><span class="badge ${item.status === 'ACTIVE' ? 'badge-active' : 'badge-archived'}">${escapeHtml(item.status)}</span></td>
                        <td>${item.count}</td>
                    </tr>
                `).join('');
            }
        }

        // Populate Search Table
        const searchTbody = document.getElementById('dashboardSearchTbody');
        if (searchTbody) {
            if (!data.frequentlySearchedCategories || data.frequentlySearchedCategories.length === 0) {
                searchTbody.innerHTML = '<tr><td colspan="3" class="text-center">No search inquiries recorded yet.</td></tr>';
            } else {
                searchTbody.innerHTML = data.frequentlySearchedCategories.map(item => `
                    <tr>
                        <td>${escapeHtml(item.category)}</td>
                        <td>${item.searchCount}</td>
                        <td>${item.avgResults}</td>
                    </tr>
                `).join('');
            }
        }
    } catch (err) {
        console.error('Failed to load dashboard:', err);
    }
}

// ===================================================================
// SCREEN B: KNOWLEDGE SEARCH
// ===================================================================
function initSearch() {
    const form = document.getElementById('searchForm');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const query = document.getElementById('searchInput').value.trim();
            const category = document.getElementById('searchCategorySelect').value || null;
            const status = document.getElementById('searchStatusSelect').value || 'ACTIVE';

            if (!query) return;

            const loading = document.getElementById('searchLoading');
            const resultsList = document.getElementById('searchResultsList');
            const metaBar = document.getElementById('searchMetaBar');
            const countText = document.getElementById('searchResultCountText');

            loading.style.display = 'block';
            resultsList.innerHTML = '';
            metaBar.style.display = 'none';

            try {
                const res = await fetch('/api/knowledge/search', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ query, category, status })
                });

                loading.style.display = 'none';

                if (!res.ok) {
                    const err = await res.json();
                    showAlert(err.message || 'Search inquiry failed.', 'danger');
                    return;
                }

                const data = await res.json();
                metaBar.style.display = 'block';
                countText.textContent = `${data.totalResults} matching passages found for query "${escapeHtml(data.query)}"`;

                if (data.results.length === 0) {
                    resultsList.innerHTML = `
                        <div class="card" style="padding: 2rem; text-align: center;">
                            <p style="font-size: 1.1rem; color: var(--text-secondary);">No matching passages found within your authorized clearance level.</p>
                            <span class="text-muted" style="font-size: 0.85rem;">Try different keywords or check if you have clearance for higher classification documents.</span>
                        </div>
                    `;
                } else {
                    resultsList.innerHTML = data.results.map(item => `
                        <div class="search-result-card">
                            <div class="search-result-header">
                                <span class="search-result-title">${escapeHtml(item.title)}</span>
                                <span class="badge badge-mode">Score: ${item.score.toFixed(1)}</span>
                            </div>
                            <div class="search-result-meta">
                                <span><strong>Category:</strong> ${escapeHtml(item.category)}</span>
                                <span><strong>Version:</strong> v${item.version}</span>
                                <span><strong>Section:</strong> ${escapeHtml(item.sectionReference || 'Section ' + item.chunkIndex)}</span>
                                ${item.pageNumber ? `<span><strong>Page:</strong> ${item.pageNumber}</span>` : ''}
                            </div>
                            <div class="search-snippet">${escapeHtml(item.snippet)}</div>
                            <div style="margin-top: 0.75rem; text-align: right;">
                                <button class="btn btn-secondary btn-sm" onclick="openSourcePreview(${item.documentId}, ${item.chunkId})">
                                    🔍 View Full Passage
                                </button>
                            </div>
                        </div>
                    `).join('');
                }
            } catch (err) {
                loading.style.display = 'none';
                console.error('Search error:', err);
                showAlert('Failed to connect to search service.', 'danger');
            }
        });
    }
}

// ===================================================================
// SCREEN C: AI RESEARCH AGENT Q&A
// ===================================================================
let currentAiResponseData = null;

function switchAiSubView(targetSubViewId) {
    document.querySelectorAll('.ai-view-tab').forEach(tab => {
        if (tab.getAttribute('data-subview') === targetSubViewId) {
            tab.classList.add('active');
        } else {
            tab.classList.remove('active');
        }
    });

    document.querySelectorAll('.ai-subview').forEach(view => {
        if (view.id === targetSubViewId) {
            view.classList.add('active');
        } else {
            view.classList.remove('active');
        }
    });
}

function extractKeywords(query) {
    if (!query) return [];
    const stopWords = new Set([
        'what', 'when', 'where', 'which', 'who', 'whom', 'whose', 'why', 'how',
        'tell', 'about', 'please', 'give', 'show', 'explain', 'describe', 'find',
        'this', 'that', 'these', 'those', 'with', 'from', 'into', 'during', 'including',
        'until', 'against', 'among', 'throughout', 'despite', 'towards', 'upon', 'concerning',
        'have', 'been', 'were', 'will', 'would', 'could', 'should', 'does', 'done'
    ]);
    return query.toLowerCase()
        .replace(/[^a-zA-Z0-9\s-]/g, ' ')
        .split(/\s+/)
        .map(w => w.trim())
        .filter(w => w.length >= 3 && !stopWords.has(w));
}

function highlightKeywordsInText(escapedText, keywords) {
    if (!escapedText || !keywords || keywords.length === 0) return escapedText;
    let result = escapedText;
    for (const kw of keywords) {
        if (kw.length < 3) continue;
        const regex = new RegExp(`(?<![a-zA-Z0-9])(${kw})(?![a-zA-Z0-9])`, 'gi');
        result = result.replace(regex, '<span class="answer-highlight">$1</span>');
    }
    return result;
}

function sanitizePassageText(text) {
    if (!text) return '';
    return text
        .replace(/\.{3,}/g, ' ')
        .replace(/Page\s+\d+\s+of\s+\d+/gi, '')
        .replace(/Table of Contents/gi, '')
        .replace(/Spec-Driven Development Workflow — Internal Documentation/gi, '')
        .replace(/[-_]{3,}/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();
}

function formatSectionReference(rawRef) {
    if (!rawRef || rawRef.trim() === '' || rawRef.startsWith('Chunk')) {
        return 'General Overview';
    }
    let clean = rawRef.trim();
    // Strip trailing punctuation
    clean = clean.replace(/[\.\s:;,-]+$/, '');
    if (clean.includes('--') || clean.startsWith('OR ') || clean.length > 45) {
        if (clean.length > 45) {
            clean = clean.substring(0, 42) + '...';
        }
    }
    return clean;
}

function formatRichAiAnswer(answerText, keywords) {
    if (!answerText) return '<p class="text-muted">No response content generated.</p>';

    const rawLines = answerText.split('\n');
    let html = '';
    let inStepList = false;

    for (let i = 0; i < rawLines.length; i++) {
        let line = rawLines[i].trim();
        if (!line) {
            if (inStepList) {
                html += '</div>';
                inStepList = false;
            }
            continue;
        }

        // Clean out noisy dot trails
        line = line.replace(/\.{3,}/g, '');

        // Check if line is a document citation header
        const docHeaderMatch = line.match(/^(?:(?:\d+[\.\)])\s+)?According to ['"]?([^'":]+)['"]?:?$/i);
        if (docHeaderMatch) {
            if (inStepList) {
                html += '</div>';
                inStepList = false;
            }
            const docName = docHeaderMatch[1].trim();
            html += `
                <div class="answer-doc-header">
                    <span>📄 Document Source: <strong>${escapeHtml(docName)}</strong></span>
                </div>
            `;
            continue;
        }

        // Check for multi-doc synthesis announcement
        if (line.toLowerCase().startsWith('synthesizing information across')) {
            if (inStepList) {
                html += '</div>';
                inStepList = false;
            }
            html += `
                <div style="font-weight: 600; color: var(--text-secondary); margin-bottom: 0.75rem;">
                    🌐 ${escapeHtml(line)}
                </div>
            `;
            continue;
        }

        // Check for numbered steps or bullet items
        const stepMatch = line.match(/^(?:[•\-\*]\s*)?(?:Step\s*(\d+)[:\s—\-]+|(\d+)[\.\)]\s+)(.*)$/i);
        const bulletMatch = line.match(/^[•\-\*]\s+(.*)$/);

        if (stepMatch) {
            if (!inStepList) {
                html += '<div class="answer-step-list">';
                inStepList = true;
            }
            const stepNum = stepMatch[1] || stepMatch[2];
            const stepBody = stepMatch[3] ? stepMatch[3].trim() : '';
            const highlightedBody = highlightKeywordsInText(escapeHtml(stepBody), keywords);

            html += `
                <div class="answer-step-item">
                    <span class="step-num-badge">${stepNum}</span>
                    <div class="step-content">${highlightedBody}</div>
                </div>
            `;
        } else if (bulletMatch) {
            if (inStepList) {
                html += '</div>';
                inStepList = false;
            }
            const bulletBody = bulletMatch[1].trim();
            const highlightedBody = highlightKeywordsInText(escapeHtml(bulletBody), keywords);

            html += `
                <div class="answer-bullet-item">
                    ${highlightedBody}
                </div>
            `;
        } else {
            if (inStepList) {
                html += '</div>';
                inStepList = false;
            }
            const highlightedText = highlightKeywordsInText(escapeHtml(line), keywords);
            html += `<p style="margin-bottom: 0.75rem;">${highlightedText}</p>`;
        }
    }

    if (inStepList) {
        html += '</div>';
    }

    return html;
}

function initAiAgent() {
    const form = document.getElementById('aiAskForm');
    const chips = document.querySelectorAll('.btn-chip');
    const questionInput = document.getElementById('aiQuestionInput');
    const quickFeedbackBtn = document.getElementById('btnQuickFeedback');
    const copyAnswerBtn = document.getElementById('btnCopyAnswer');
    const inspectEvidenceBtn = document.getElementById('btnInspectEvidence');

    // Sub-view tab switching
    document.querySelectorAll('.ai-view-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            const subview = tab.getAttribute('data-subview');
            if (subview) switchAiSubView(subview);
        });
    });

    if (inspectEvidenceBtn) {
        inspectEvidenceBtn.addEventListener('click', () => {
            switchAiSubView('evidenceView');
        });
    }

    if (copyAnswerBtn) {
        copyAnswerBtn.addEventListener('click', async () => {
            if (!currentAiResponseData || !currentAiResponseData.answer) return;
            try {
                await navigator.clipboard.writeText(currentAiResponseData.answer);
                const originalHtml = copyAnswerBtn.innerHTML;
                copyAnswerBtn.innerHTML = '✅ Copied to Clipboard!';
                setTimeout(() => {
                    copyAnswerBtn.innerHTML = originalHtml;
                }, 2000);
            } catch (err) {
                const textarea = document.createElement('textarea');
                textarea.value = currentAiResponseData.answer;
                document.body.appendChild(textarea);
                textarea.select();
                document.execCommand('copy');
                document.body.removeChild(textarea);
                showAlert('Answer copied to clipboard!', 'success');
            }
        });
    }

    chips.forEach(chip => {
        chip.addEventListener('click', () => {
            const query = chip.getAttribute('data-query');
            if (questionInput) questionInput.value = query;
        });
    });

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const question = questionInput.value.trim();
            if (!question) return;

            const loading = document.getElementById('aiLoading');
            const container = document.getElementById('aiResponseContainer');

            loading.style.display = 'block';
            container.style.display = 'none';

            try {
                const res = await fetch('/api/knowledge/ask', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ question })
                });

                loading.style.display = 'none';

                if (!res.ok) {
                    const err = await res.json();
                    showAlert(err.message || 'AI Agent request failed.', 'danger');
                    return;
                }

                const data = await res.json();
                currentAiResponseData = data;
                renderAiResponse(data, question);
                switchAiSubView('answerView');
                container.style.display = 'block';

                container.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

            } catch (err) {
                loading.style.display = 'none';
                console.error('AI Ask error:', err);
                showAlert('Failed to contact AI Agent service.', 'danger');
            }
        });
    }

    if (quickFeedbackBtn) {
        quickFeedbackBtn.addEventListener('click', () => {
            switchTab('feedbackSection');
            const qInput = document.getElementById('feedbackQuestion');
            if (qInput && questionInput) qInput.value = questionInput.value;
        });
    }
}

function renderAiResponse(data, query) {
    const answerEl = document.getElementById('aiAnswerContent');
    const modeBadge = document.getElementById('aiModeBadge');
    const abstainedBadge = document.getElementById('aiAbstainedBadge');
    const reasoningNotesEl = document.getElementById('aiReasoningNotes');
    const citationsGrid = document.getElementById('citationsGrid');
    const citationCountBadge = document.getElementById('citationCountBadge');
    const evidenceTabCountEl = document.getElementById('evidenceTabCount');
    const subtitleEl = document.getElementById('responseQuestionSubtitle');

    const effectiveQuery = data.question || query || '';
    const keywords = extractKeywords(effectiveQuery);

    // Subtitle
    if (subtitleEl) {
        subtitleEl.textContent = effectiveQuery ? `Query: "${effectiveQuery}"` : '';
    }

    // Answer Rendering
    if (data.abstained) {
        answerEl.innerHTML = `
            <div class="abstention-notice">
                <div class="notice-icon">🛑</div>
                <div class="notice-content">
                    <h4>Direct Answer Abstained (Integrity Guardrail)</h4>
                    <p>${escapeHtml(data.answer)}</p>
                </div>
            </div>
        `;
    } else {
        answerEl.innerHTML = formatRichAiAnswer(data.answer, keywords);
    }

    // Mode Badge
    modeBadge.textContent = data.mode;
    if (data.mode === 'REAL_AI') {
        modeBadge.className = 'badge badge-success';
    } else if (data.mode === 'SIMULATION_FALLBACK') {
        modeBadge.className = 'badge badge-admin';
    } else {
        modeBadge.className = 'badge badge-mode';
    }

    // Abstention Badge
    if (data.abstained) {
        abstainedBadge.style.display = 'inline-block';
        abstainedBadge.className = 'badge badge-admin';
        abstainedBadge.textContent = 'ABSTAINED';
    } else {
        abstainedBadge.style.display = 'inline-block';
        abstainedBadge.className = 'badge badge-success';
        abstainedBadge.textContent = 'VERIFIED EVIDENCE';
    }

    // Reasoning Notes
    if (reasoningNotesEl) {
        reasoningNotesEl.textContent = data.reasoningNotes || 'Workflow completed.';
    }

    // Citations Count
    const srcCount = (data.sources || []).length;
    if (citationCountBadge) {
        citationCountBadge.textContent = `${srcCount} Sources Cited`;
    }
    if (evidenceTabCountEl) {
        evidenceTabCountEl.textContent = srcCount;
    }

    // Render Evidence Cards
    if (!data.sources || data.sources.length === 0) {
        citationsGrid.innerHTML = `
            <div style="padding: 2.5rem 1rem; text-align: center; color: var(--text-muted); background: #f8fafc; border-radius: var(--radius-md); border: 1px dashed var(--border-color);">
                <div style="font-size: 2.2rem; margin-bottom: 0.5rem;">🔒</div>
                <strong style="font-size: 1rem; color: var(--text-secondary);">No source citations attached.</strong>
                <p style="font-size: 0.88rem; margin: 0.35rem 0 0 0;">
                    ${data.abstained ? 'The agent strictly refrained from generating unverified claims without approved document backing.' : 'No matching passages met the relevance threshold for this inquiry.'}
                </p>
            </div>
        `;
    } else {
        citationsGrid.innerHTML = data.sources.map(src => {
            const cleanExcerpt = sanitizePassageText(src.excerpt);
            const highlightedExcerpt = highlightKeywordsInText(escapeHtml(cleanExcerpt), keywords);
            const sectionRef = formatSectionReference(src.sectionReference);

            return `
                <div class="evidence-passage-card">
                    <div class="evidence-card-header">
                        <div class="evidence-title-group">
                            <span class="evidence-doc-title">📄 ${escapeHtml(src.documentTitle)}</span>
                            <span class="badge badge-employee" style="font-size: 0.75rem;">v${src.documentVersion}</span>
                            <span class="evidence-section-pill">📍 ${escapeHtml(sectionRef)}</span>
                        </div>
                        <button type="button" class="btn btn-secondary btn-sm" onclick="openSourcePreviewByDocAndChunk(${src.documentId}, ${src.chunkIndex})">
                            🔍 Inspect Full Chunk
                        </button>
                    </div>
                    <div class="evidence-quote-box">
                        "${highlightedExcerpt}"
                    </div>
                    <div class="evidence-card-footer">
                        <span>Chunk #${src.chunkIndex} in MySQL Repository</span>
                        <span class="badge-verified">✓ Access Clearance Verified</span>
                    </div>
                </div>
            `;
        }).join('');
    }
}

// ===================================================================
// SCREEN D: DOCUMENT MANAGEMENT
// ===================================================================
function initDocumentManagement() {
    const uploadForm = document.getElementById('documentUploadForm');
    const refreshBtn = document.getElementById('btnRefreshDocs');
    const catFilter = document.getElementById('docListCategoryFilter');
    const statusFilter = document.getElementById('docListStatusFilter');

    if (uploadForm) {
        uploadForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const title = document.getElementById('uploadTitle').value.trim();
            const description = document.getElementById('uploadDescription').value.trim();
            const category = document.getElementById('uploadCategory').value;
            const accessLevel = document.getElementById('uploadAccessLevel').value;
            const fileInput = document.getElementById('uploadFile');

            if (!fileInput.files || fileInput.files.length === 0) {
                showAlert('Please select a file to upload.', 'danger');
                return;
            }

            const formData = new FormData();
            formData.append('title', title);
            formData.append('description', description);
            formData.append('category', category);
            formData.append('accessLevel', accessLevel);
            formData.append('file', fileInput.files[0]);

            const submitBtn = document.getElementById('uploadSubmitBtn');
            submitBtn.disabled = true;
            submitBtn.innerHTML = '<span>Uploading & Indexing...</span>';

            try {
                const res = await fetch('/api/knowledge/documents', {
                    method: 'POST',
                    body: formData
                });

                submitBtn.disabled = false;
                submitBtn.innerHTML = '<span>Upload and Index Document</span>';

                if (res.ok) {
                    const doc = await res.json();
                    showAlert(`Document '${doc.title}' uploaded and indexed into ${doc.chunkCount} chunks!`, 'success');
                    uploadForm.reset();
                    loadDocumentsTable();
                } else {
                    const err = await res.json();
                    showAlert(err.message || 'Document upload rejected.', 'danger');
                }
            } catch (err) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '<span>Upload and Index Document</span>';
                console.error('Upload error:', err);
                showAlert('Error connecting to upload endpoint.', 'danger');
            }
        });
    }

    if (refreshBtn) refreshBtn.addEventListener('click', loadDocumentsTable);
    if (catFilter) catFilter.addEventListener('change', loadDocumentsTable);
    if (statusFilter) statusFilter.addEventListener('change', loadDocumentsTable);
}

async function loadDocumentsTable() {
    const catFilter = document.getElementById('docListCategoryFilter')?.value || '';
    const statusFilter = document.getElementById('docListStatusFilter')?.value || 'ACTIVE';
    const tbody = document.getElementById('documentsTableTbody');

    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="9" class="text-center">Loading accessible documents...</td></tr>';

    let url = `/api/knowledge/documents?status=${statusFilter}`;
    if (catFilter) url += `&category=${catFilter}`;

    try {
        const res = await fetch(url);
        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center">Failed to load documents.</td></tr>';
            return;
        }

        const documents = await res.json();
        AppState.accessibleDocuments = documents;

        if (documents.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center">No documents found matching the filter criteria.</td></tr>';
            return;
        }

        tbody.innerHTML = documents.map(doc => {
            const accessBadgeClass = `badge-${doc.accessLevel.toLowerCase()}`;
            const statusBadgeClass = doc.status === 'ACTIVE' ? 'badge-active' : 'badge-archived';
            const dateStr = doc.updatedAt ? new Date(doc.updatedAt).toLocaleDateString() : '-';

            return `
                <tr>
                    <td><strong>#${doc.id}</strong></td>
                    <td>${escapeHtml(doc.title)}</td>
                    <td>${escapeHtml(doc.category)}</td>
                    <td><span class="badge ${accessBadgeClass}">${escapeHtml(doc.accessLevel)}</span></td>
                    <td>v${doc.version}</td>
                    <td><span class="badge ${statusBadgeClass}">${escapeHtml(doc.status)}</span></td>
                    <td>${doc.chunkCount}</td>
                    <td>${dateStr}</td>
                    <td>
                        <div style="display: flex; gap: 0.35rem;">
                            <button class="btn btn-secondary btn-sm" onclick="viewDocumentDetails(${doc.id})">Details</button>
                            ${(AppState.currentUser?.role === 'ROLE_ADMIN' && doc.status === 'ACTIVE') ? `
                                <button class="btn btn-secondary btn-sm" onclick="reindexDocument(${doc.id})">Re-index</button>
                                <button class="btn btn-secondary btn-sm" style="color: var(--danger);" onclick="archiveDocument(${doc.id})">Archive</button>
                            ` : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        console.error('Load documents error:', err);
        tbody.innerHTML = '<tr><td colspan="9" class="text-center">Error loading documents.</td></tr>';
    }
}

async function viewDocumentDetails(id) {
    try {
        const res = await fetch(`/api/knowledge/documents/${id}`);
        if (!res.ok) {
            const err = await res.json();
            showAlert(err.message || 'Cannot view document details.', 'danger');
            return;
        }
        const data = await res.json();
        const doc = data.document;
        const chunks = data.chunks;

        let content = `Title: ${doc.title}\nCategory: ${doc.category}\nAccess Clearance: ${doc.accessLevel}\nVersion: ${doc.version}\nStatus: ${doc.status}\nOwner: ${doc.owner}\n\n`;
        content += `Indexed Passages (${chunks.length}):\n`;
        chunks.forEach(c => {
            content += `\n--- [Chunk ${c.chunkIndex}: ${c.sectionReference || 'Section ' + c.chunkIndex}] ---\n${c.content}\n`;
        });

        openModal(doc.title, `Version ${doc.version} | ${chunks.length} Chunks`, content, doc.accessLevel);
    } catch (err) {
        console.error('View doc details error:', err);
    }
}

async function reindexDocument(id) {
    if (!confirm(`Are you sure you want to re-index document #${id}? This will bump the document version and regenerate active chunks.`)) {
        return;
    }
    try {
        const res = await fetch(`/api/knowledge/documents/${id}/reindex`, { method: 'PUT' });
        if (res.ok) {
            const updated = await res.json();
            showAlert(`Document #${id} successfully re-indexed to version ${updated.version}!`, 'success');
            loadDocumentsTable();
        } else {
            const err = await res.json();
            showAlert(err.message || 'Failed to re-index document.', 'danger');
        }
    } catch (err) {
        console.error('Reindex error:', err);
    }
}

async function archiveDocument(id) {
    if (!confirm(`Are you sure you want to safely archive document #${id}? Archived documents are excluded from search and Q&A.`)) {
        return;
    }
    try {
        const res = await fetch(`/api/knowledge/documents/${id}`, { method: 'DELETE' });
        if (res.ok) {
            showAlert(`Document #${id} has been safely archived.`, 'success');
            loadDocumentsTable();
        } else {
            const err = await res.json();
            showAlert(err.message || 'Failed to archive document.', 'danger');
        }
    } catch (err) {
        console.error('Archive error:', err);
    }
}

// ===================================================================
// SCREEN E: SOURCE PREVIEW MODAL
// ===================================================================
function initModal() {
    const modal = document.getElementById('sourcePreviewModal');
    const closeBtn = document.getElementById('modalCloseBtn');
    const dismissBtn = document.getElementById('modalDismissBtn');

    const closeModal = () => { if (modal) modal.style.display = 'none'; };
    if (closeBtn) closeBtn.onclick = closeModal;
    if (dismissBtn) dismissBtn.onclick = closeModal;
}

function openModal(title, meta, text, clearance) {
    const modal = document.getElementById('sourcePreviewModal');
    document.getElementById('modalDocTitle').textContent = title;
    document.getElementById('modalMeta').textContent = meta;
    document.getElementById('modalChunkContent').textContent = text;
    document.getElementById('modalClearanceTag').textContent = `Clearance: ${clearance || 'AUTHORIZED'}`;
    if (modal) modal.style.display = 'flex';
}

async function openSourcePreview(documentId, chunkId) {
    try {
        const res = await fetch(`/api/knowledge/documents/${documentId}/chunks/${chunkId}`);
        if (!res.ok) {
            const err = await res.json();
            showAlert(err.message || 'Clearance check failed. You cannot preview this source.', 'danger');
            return;
        }
        const chunk = await res.json();
        openModal(
            `Source Preview (Document #${documentId})`,
            `${chunk.sectionReference || 'Chunk ' + chunk.chunkIndex} | Version ${chunk.documentVersion}`,
            chunk.content,
            'Verified Access'
        );
    } catch (err) {
        console.error('Preview error:', err);
    }
}

async function openSourcePreviewByDocAndChunk(documentId, chunkIndex) {
    try {
        const res = await fetch(`/api/knowledge/documents/${documentId}`);
        if (!res.ok) {
            const err = await res.json();
            showAlert(err.message || 'Access restricted.', 'danger');
            return;
        }
        const data = await res.json();
        const chunk = data.chunks.find(c => c.chunkIndex === chunkIndex) || data.chunks[0];
        if (chunk) {
            openSourcePreview(documentId, chunk.id);
        }
    } catch (err) {
        console.error('Preview error:', err);
    }
}

// ===================================================================
// SCREEN F: FEEDBACK
// ===================================================================
function initFeedback() {
    const starBtns = document.querySelectorAll('.star-btn');
    const ratingInput = document.getElementById('feedbackRatingValue');
    const form = document.getElementById('feedbackForm');

    starBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const val = parseInt(btn.getAttribute('data-value'), 10);
            if (ratingInput) ratingInput.value = val;
            starBtns.forEach(b => {
                const bVal = parseInt(b.getAttribute('data-value'), 10);
                b.classList.toggle('active', bVal <= val);
            });
        });
    });

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const documentId = parseInt(document.getElementById('feedbackDocSelect').value, 10);
            const question = document.getElementById('feedbackQuestion').value.trim();
            const answerReference = document.getElementById('feedbackAnswerRef').value.trim();
            const rating = parseInt(ratingInput.value, 10);
            const comment = document.getElementById('feedbackComment').value.trim();

            if (!documentId) {
                showAlert('Please select the referenced document.', 'danger');
                return;
            }

            try {
                const res = await fetch('/api/knowledge/feedback', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ documentId, question, answerReference, rating, comment })
                });

                if (res.ok) {
                    const fb = await res.json();
                    showAlert(fb.message || 'Feedback recorded successfully!', 'success');
                    form.reset();
                    ratingInput.value = 5;
                    starBtns.forEach(b => b.classList.add('active'));
                } else {
                    const err = await res.json();
                    showAlert(err.message || 'Feedback submission rejected.', 'danger');
                }
            } catch (err) {
                console.error('Feedback submit error:', err);
                showAlert('Could not communicate with feedback service.', 'danger');
            }
        });
    }
}

async function populateFeedbackDocOptions() {
    const select = document.getElementById('feedbackDocSelect');
    if (!select) return;

    try {
        const res = await fetch('/api/knowledge/documents?status=ACTIVE');
        if (!res.ok) return;
        const docs = await res.json();
        select.innerHTML = '<option value="">Select an accessible document...</option>' +
            docs.map(d => `<option value="${d.id}">#${d.id} - ${escapeHtml(d.title)} (${d.accessLevel})</option>`).join('');
    } catch (err) {
        console.error('Failed to populate doc options:', err);
    }
}

// ===================================================================
// SCREEN G: ANALYTICS
// ===================================================================
function initAnalytics() {
    const roleSimBtn = document.getElementById('btnTestRoleAccess');
    if (roleSimBtn) {
        roleSimBtn.addEventListener('click', runRoleAccessSimulation);
    }
}

async function loadAnalyticsData() {
    try {
        const res = await fetch('/api/knowledge/analytics');
        if (!res.ok) return;
        const data = await res.json();

        // 1. Feedback ratings grouped by document
        const fbTbody = document.getElementById('analyticsFeedbackTbody');
        if (fbTbody) {
            if (!data.feedbackRatingsByDocument || data.feedbackRatingsByDocument.length === 0) {
                fbTbody.innerHTML = '<tr><td colspan="6" class="text-center">No feedback ratings recorded yet.</td></tr>';
            } else {
                fbTbody.innerHTML = data.feedbackRatingsByDocument.map(item => `
                    <tr>
                        <td>#${item.documentId}</td>
                        <td>${escapeHtml(item.title)}</td>
                        <td>${item.feedbackCount}</td>
                        <td><strong>⭐ ${item.averageRating.toFixed(1)}</strong></td>
                        <td>${item.fiveStarCount}</td>
                        <td>${item.lowRatingCount}</td>
                    </tr>
                `).join('');
            }
        }

        // 2. Documents awaiting re-indexing
        const reindexTbody = document.getElementById('analyticsReindexTbody');
        if (reindexTbody) {
            if (!data.documentsAwaitingReindex || data.documentsAwaitingReindex.length === 0) {
                reindexTbody.innerHTML = '<tr><td colspan="5" class="text-center">All documents are currently up-to-date.</td></tr>';
            } else {
                reindexTbody.innerHTML = data.documentsAwaitingReindex.map(item => `
                    <tr>
                        <td>#${item.id}</td>
                        <td>${escapeHtml(item.title)}</td>
                        <td>${escapeHtml(item.category)}</td>
                        <td>v${item.version}</td>
                        <td>${new Date(item.updatedAt).toLocaleDateString()}</td>
                    </tr>
                `).join('');
            }
        }

    } catch (err) {
        console.error('Load analytics error:', err);
    }
}

async function runRoleAccessSimulation() {
    const role = document.getElementById('analyticsRoleSelect')?.value || 'ROLE_EMPLOYEE';
    const tbody = document.getElementById('analyticsRoleAccessTbody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="5" class="text-center">Simulating role access policy...</td></tr>';

    try {
        const res = await fetch(`/api/knowledge/analytics/role-access?role=${role}`);
        if (!res.ok) {
            const err = await res.json();
            tbody.innerHTML = `<tr><td colspan="5" class="text-center" style="color: var(--danger);">${escapeHtml(err.message || 'Access restricted to administrators.')}</td></tr>`;
            return;
        }

        const docs = await res.json();
        if (docs.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No documents visible to this role.</td></tr>';
            return;
        }

        tbody.innerHTML = docs.map(d => `
            <tr>
                <td>#${d.id}</td>
                <td>${escapeHtml(d.title)}</td>
                <td>${escapeHtml(d.category)}</td>
                <td><span class="badge badge-${d.accessLevel.toLowerCase()}">${escapeHtml(d.accessLevel)}</span></td>
                <td>v${d.version}</td>
            </tr>
        `).join('');

    } catch (err) {
        console.error('Role sim error:', err);
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">Error simulating role access.</td></tr>';
    }
}

// ===================================================================
// SCREEN G: USERS DIRECTORY (ADMIN ONLY)
// ===================================================================
function initUsersDirectory() {
    const refreshBtn = document.getElementById('btnRefreshUsers');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', loadUsersDirectory);
    }
}

async function loadUsersDirectory() {
    const tbody = document.getElementById('usersTableTbody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="6" class="text-center">Loading registered user accounts...</td></tr>';

    try {
        const res = await fetch('/api/auth/users');
        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Access Restricted: Only Administrators can inspect the user directory.</td></tr>';
            return;
        }

        const users = await res.json();
        if (users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">No user accounts found.</td></tr>';
            return;
        }

        tbody.innerHTML = users.map(u => {
            const isAdm = u.role === 'ROLE_ADMIN';
            const roleBadgeClass = isAdm ? 'badge-admin' : 'badge-employee';
            const scopeText = isAdm
                ? 'Tier 3: Public, Employee, HR, Admin (Unrestricted)'
                : 'Tier 1: Public, Employee (Approved Corporate Policies)';

            return `
                <tr>
                    <td><strong>#${u.id}</strong></td>
                    <td><strong>${escapeHtml(u.fullName)}</strong></td>
                    <td><code>${escapeHtml(u.username)}</code></td>
                    <td><span class="badge ${roleBadgeClass}">${escapeHtml(u.role)}</span></td>
                    <td style="font-size: 0.85rem; color: var(--text-secondary);">${scopeText}</td>
                    <td><span class="badge badge-active">Active</span></td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        console.error('Load users directory error:', err);
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">Failed to load user directory.</td></tr>';
    }
}

// Utility: HTML escaping to prevent XSS
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}
