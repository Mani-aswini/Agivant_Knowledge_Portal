/**
 * Enterprise Knowledge Portal - Vanilla JavaScript Application
 * Handles authentication, role-based functionality access, document management,
 * semantic search, and the AI research agent workflow.
 */

// Application State
const AppState = {
    currentUser: null,
    activeSection: 'dashboardSection',
    accessibleDocuments: [],
    feedbackItems: []
};


// DOM Ready Initialization
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initNavigation();
    initAuthPortal();
    initDashboard();
    initSearch();
    initAiAgent();
    initDocumentManagement();
    initFeedback();
    initAnalytics();
    initUsersDirectory();
    initAuditLogs();
    initProcessingJobs();
    initSystemSettings();
    initEmployees();
    initModal();

    // Check if user has an existing active session, or display Auth Portal
    checkSessionOrShowAuth();
});

// ===================================================================
// Dark / Light Theme Management
// ===================================================================
function initTheme() {
    let savedTheme = 'light';
    try {
        savedTheme = localStorage.getItem('agivant_portal_theme') || 'light';
    } catch (e) {}
    applyTheme(savedTheme);

    const btnThemeToggle = document.getElementById('btnThemeToggle');
    if (btnThemeToggle) {
        btnThemeToggle.addEventListener('click', () => {
            const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
            const nextTheme = (currentTheme === 'dark') ? 'light' : 'dark';
            applyTheme(nextTheme);
        });
    }

    const btnThemeToggleAuth = document.getElementById('btnThemeToggleAuth');
    if (btnThemeToggleAuth) {
        btnThemeToggleAuth.addEventListener('click', () => {
            const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
            const nextTheme = (currentTheme === 'dark') ? 'light' : 'dark';
            applyTheme(nextTheme);
        });
    }
}

function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    try {
        localStorage.setItem('agivant_portal_theme', theme);
    } catch (e) {}

    const isDark = (theme === 'dark');

    // Update Header Toggle Button
    const btnThemeToggle = document.getElementById('btnThemeToggle');
    if (btnThemeToggle) {
        const sunIcon = btnThemeToggle.querySelector('.theme-icon-sun');
        const moonIcon = btnThemeToggle.querySelector('.theme-icon-moon');
        const textLabel = document.getElementById('themeToggleText');

        if (sunIcon) sunIcon.style.display = isDark ? 'inline-block' : 'none';
        if (moonIcon) moonIcon.style.display = isDark ? 'none' : 'inline-block';
        if (textLabel) textLabel.textContent = isDark ? 'Light' : 'Dark';
        btnThemeToggle.setAttribute('title', isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme');
        btnThemeToggle.setAttribute('aria-label', isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme');
    }

    // Update Auth Screen Toggle Button
    const btnThemeToggleAuth = document.getElementById('btnThemeToggleAuth');
    if (btnThemeToggleAuth) {
        const sunIconAuth = btnThemeToggleAuth.querySelector('.theme-icon-sun');
        const moonIconAuth = btnThemeToggleAuth.querySelector('.theme-icon-moon');
        const textLabelAuth = document.getElementById('themeToggleAuthText');

        if (sunIconAuth) sunIconAuth.style.display = isDark ? 'inline-block' : 'none';
        if (moonIconAuth) moonIconAuth.style.display = isDark ? 'none' : 'inline-block';
        if (textLabelAuth) textLabelAuth.textContent = isDark ? 'Light' : 'Dark';
        btnThemeToggleAuth.setAttribute('title', isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme');
        btnThemeToggleAuth.setAttribute('aria-label', isDark ? 'Switch to Light Theme' : 'Switch to Dark Theme');
    }
}

// ===================================================================
// Authentication & Role-Based Access Control
// ===================================================================
function initAuthPortal() {
    // Toggle Password Visibility (Eye Icon)
    const btnTogglePassword = document.getElementById('btnTogglePassword');
    const loginPassword = document.getElementById('loginPassword');
    if (btnTogglePassword && loginPassword) {
        btnTogglePassword.addEventListener('click', (e) => {
            e.preventDefault();
            const isPassword = loginPassword.type === 'password';
            loginPassword.type = isPassword ? 'text' : 'password';

            const eyeShow = btnTogglePassword.querySelector('.eye-show');
            const eyeHide = btnTogglePassword.querySelector('.eye-hide');
            if (eyeShow && eyeHide) {
                eyeShow.style.display = isPassword ? 'none' : 'block';
                eyeHide.style.display = isPassword ? 'block' : 'none';
            }
            btnTogglePassword.setAttribute('title', isPassword ? 'Hide password' : 'Show password');
            btnTogglePassword.setAttribute('aria-label', isPassword ? 'Hide password' : 'Show password');
            loginPassword.focus();
        });
    }

    const loginUsernameInput = document.getElementById('loginUsername');
    if (loginUsernameInput) {
        loginUsernameInput.addEventListener('input', () => {
            clearAuthAlert();
            loginUsernameInput.style.borderColor = '';
        });
    }

    // Unified Single Login Form
    const formUnifiedLogin = document.getElementById('formUnifiedLogin');
    if (formUnifiedLogin) {
        formUnifiedLogin.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearAuthAlert();

            const usernameInput = document.getElementById('loginUsername');
            const username = usernameInput ? usernameInput.value.trim() : '';
            const password = document.getElementById('loginPassword').value;
            const submitBtn = document.getElementById('btnLoginSubmit');

            if (!username || !password) {
                showAuthAlert('Please enter both username and password.');
                return;
            }

            // Enforce lowercase usernames: employee usernames are strictly registered in lowercase
            if (/[A-Z]/.test(username)) {
                showAuthAlert(`Usernames are strictly lowercase. Please enter your username in lowercase (e.g. "${username.toLowerCase()}").`);
                if (usernameInput) {
                    usernameInput.style.borderColor = '#ef4444';
                    usernameInput.focus();
                }
                return;
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = `<span>Signing in...</span>`;

            try {
                const res = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });

                submitBtn.disabled = false;
                submitBtn.innerHTML = `
                    <span>Sign In</span>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                        <polyline points="12 5 19 12 12 19"></polyline>
                    </svg>
                `;

                if (!res.ok) {
                    showAuthAlert('Invalid username and password.');
                    return;
                }

                const user = await res.json();
                onLoginSuccess(user, true);

            } catch (err) {
                submitBtn.disabled = false;
                submitBtn.innerHTML = `
                    <span>Sign In</span>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="5" y1="12" x2="19" y2="12"></line>
                        <polyline points="12 5 19 12 12 19"></polyline>
                    </svg>
                `;
                console.error('Unified login error:', err);
                showAuthAlert('Invalid username and password.');
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

            // Clear login input fields completely
            const usernameInput = document.getElementById('loginUsername');
            const passwordInput = document.getElementById('loginPassword');
            if (usernameInput) usernameInput.value = '';
            if (passwordInput) passwordInput.value = '';

            AppState.currentUser = null;
            // Clean page reload back to fresh login state
            window.location.reload();
        });
    }
}

function showAuthAlert(msg) {
    const alertBox = document.getElementById('authAlert');
    const alertText = document.getElementById('authAlertText');
    if (alertBox && alertText) {
        alertText.textContent = msg || 'Invalid username and password.';
        alertBox.style.display = 'flex';
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

    // Reset all chat states to prevent cross-user leakage
    activeChatUser = null;
    userConversations = [];
    userConversationTurns = [];
    activeConversationId = null;
    const stream = document.getElementById('chatMessagesStream');
    if (stream) stream.innerHTML = '';
    const sitemapList = document.getElementById('chatSitemapList');
    if (sitemapList) sitemapList.innerHTML = '';
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
        let roleLabel = 'Staff Employee';
        if (user.role === 'ROLE_ADMIN') {
            roleLabel = 'Portal Administrator';
        } else if (user.role === 'ROLE_HR') {
            roleLabel = 'HR Manager';
        }
        showAlert(`Signed in as ${user.fullName} (${roleLabel}).`, 'success');
    }

    // Default to dashboard
    switchTab('dashboardSection');
    refreshActiveSection();
}

function applyUserPermissions(user) {
    if (!user) return;
    const isAdmin = user.role === 'ROLE_ADMIN';
    const isHr = user.role === 'ROLE_HR';
    const isAdminOrHr = isAdmin || isHr;

    // 1. Header Profile Badge & Brand Titles
    const nameEl = document.getElementById('userName');
    const roleBadgeEl = document.getElementById('userRoleBadge');
    const avatarEl = document.getElementById('userAvatar');
    const clearancePill = document.getElementById('userClearancePill');
    const headerTitle = document.getElementById('portalHeaderTitle');
    const headerSubtitle = document.getElementById('portalHeaderSubtitle');

    if (nameEl) nameEl.textContent = user.fullName;
    if (avatarEl) avatarEl.textContent = user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U';

    if (roleBadgeEl) {
        if (isAdmin) {
            roleBadgeEl.textContent = 'ADMINISTRATOR';
            roleBadgeEl.className = 'badge badge-admin';
        } else if (isHr) {
            roleBadgeEl.textContent = 'HUMAN RESOURCES';
            roleBadgeEl.className = 'badge badge-hr';
        } else {
            roleBadgeEl.textContent = 'EMPLOYEE';
            roleBadgeEl.className = 'badge badge-employee';
        }
    }

    if (headerTitle) {
        headerTitle.textContent = 'Agivant Knowledge Portal';
    }
    if (headerSubtitle) {
        if (isAdmin) {
            headerSubtitle.textContent = 'Admin Portal & Knowledge Management';
        } else if (isHr) {
            headerSubtitle.textContent = 'HR Management & Knowledge Portal';
        } else {
            headerSubtitle.textContent = 'AI Assistant & Knowledge Base';
        }
    }

    if (clearancePill) {
        clearancePill.style.display = 'none';
        clearancePill.remove();
    }

    // 2. Navigation Tab Visibility & Titles
    const navMoreDropdown = document.getElementById('navMoreDropdown');
    const tabAnalytics = document.getElementById('tabAnalytics');
    const tabUsers = document.getElementById('tabUsers');
    const tabAuditLogs = document.getElementById('tabAuditLogs');
    const tabProcessingJobs = document.getElementById('tabProcessingJobs');
    const tabSystemSettings = document.getElementById('tabSystemSettings');
    const tabFeedback = document.getElementById('tabFeedback');
    const tabEmployees = document.getElementById('tabEmployees');

    if (navMoreDropdown) navMoreDropdown.style.display = 'none';
    if (tabAnalytics) tabAnalytics.style.display = isAdminOrHr ? 'flex' : 'none';
    if (tabUsers) tabUsers.style.display = isAdmin ? 'flex' : 'none';
    if (tabAuditLogs) tabAuditLogs.style.display = isAdmin ? 'flex' : 'none';
    if (tabProcessingJobs) tabProcessingJobs.style.display = isAdmin ? 'flex' : 'none';
    if (tabSystemSettings) tabSystemSettings.style.display = isAdmin ? 'flex' : 'none';
    if (tabFeedback) tabFeedback.style.display = isAdminOrHr ? 'flex' : 'none';
    if (tabEmployees) tabEmployees.style.display = isAdminOrHr ? 'flex' : 'none';

    const sitemapGroupManagement = document.getElementById('sitemapGroupManagement');
    if (sitemapGroupManagement) {
        sitemapGroupManagement.style.display = isAdminOrHr ? 'block' : 'none';
    }

    const tabSearchLabel = document.getElementById('tabSearchLabel');
    if (tabSearchLabel) {
        tabSearchLabel.textContent = isAdminOrHr ? 'Knowledge Search' : 'Search Documents';
    }

    const tabAiAgentLabel = document.getElementById('tabAiAgentLabel');
    if (tabAiAgentLabel) {
        tabAiAgentLabel.textContent = 'AI Assistant';
    }

    const tabDocsLabel = document.getElementById('tabDocumentsLabel');
    if (tabDocsLabel) {
        tabDocsLabel.textContent = isAdminOrHr ? 'Document Management' : 'Company Documents';
        const parentBtn = tabDocsLabel.closest('.nav-tab');
        if (parentBtn) {
            parentBtn.setAttribute('title', isAdminOrHr ? 'Document Ingestion & Management' : 'Company Documents');
        }
    }

    // Role-tailored Section Headings
    const searchSectionHeading = document.getElementById('searchSectionHeading');
    const searchSectionDesc = document.getElementById('searchSectionDesc');
    if (searchSectionHeading) {
        searchSectionHeading.textContent = isAdminOrHr ? 'Knowledge Search' : 'Search Documents';
    }
    if (searchSectionDesc) {
        searchSectionDesc.textContent = isAdminOrHr
            ? 'Query approved enterprise knowledge assets with keyword and vector relevance ranking.'
            : 'Search approved company policies, technical guides, and common questions.';
    }

    const aiSectionHeading = document.getElementById('aiSectionHeading');
    const aiSectionDesc = document.getElementById('aiSectionDesc');
    if (aiSectionHeading) {
        aiSectionHeading.textContent = 'Agivant AI Assistant';
    }
    if (aiSectionDesc) {
        aiSectionDesc.textContent = isAdminOrHr
            ? 'Ask questions to the RAG AI agent with multi-document evidence synthesis and citation verification.'
            : 'Ask about company policies, technical guides, or workplace procedures.';
    }

    const docsHeading = document.getElementById('documentsSectionHeading');
    const docsDesc = document.getElementById('documentsSectionDesc');
    if (docsHeading) {
        docsHeading.textContent = isAdminOrHr ? 'Document Management' : 'Company Documents';
    }
    if (docsDesc) {
        docsDesc.textContent = isAdminOrHr
            ? 'Upload, segment, re-index, and safely archive approved enterprise documents.'
            : 'Browse and view approved company policies, technical guides, and common questions.';
    }

    // 3. Document Management Functionality Isolation (HR & Admin can upload)
    const uploadCard = document.getElementById('documentUploadCard');
    const employeeNotice = document.getElementById('employeeReadonlyNotice');

    if (uploadCard) {
        uploadCard.style.display = isAdminOrHr ? 'block' : 'none';
    }
    if (employeeNotice) {
        employeeNotice.style.display = isAdminOrHr ? 'none' : 'block';
    }

    // 4. Employee Portal Dashboard vs Administrator / HR Dashboard View Isolation
    const empDashboardView = document.getElementById('employeeDashboardView');
    const adminDashboardView = document.getElementById('adminDashboardView');
    if (empDashboardView && adminDashboardView) {
        if (isAdminOrHr) {
            empDashboardView.style.display = 'none';
            adminDashboardView.style.display = 'block';
        } else {
            empDashboardView.style.display = 'flex';
            adminDashboardView.style.display = 'none';
        }
    }

    // 5. Tailor Dashboard Section Header
    const adminDashHeader = document.querySelector('#adminDashboardView .section-header h2');
    const adminDashDesc = document.querySelector('#adminDashboardView .section-header .section-desc');
    if (adminDashHeader) {
        if (isHr) {
            adminDashHeader.textContent = `HR Management Overview — ${user.fullName}`;
        } else if (isAdmin) {
            adminDashHeader.textContent = `Portal Administrator Overview — ${user.fullName}`;
        } else {
            adminDashHeader.textContent = 'Portal Overview & Metrics';
        }
    }
    if (adminDashDesc) {
        if (isHr) {
            adminDashDesc.textContent = 'Management dashboard for HR documents, personnel records, and department policies.';
        } else if (isAdmin) {
            adminDashDesc.textContent = 'Real-time status of enterprise knowledge assets, search analytics, and indexing operations.';
        }
    }

    // 6. Load isolated user conversation history for the AI Assistant Chatbot
    if (typeof loadUserChatHistory === 'function' && user.username) {
        loadUserChatHistory(user.username);
    }
}

// ===================================================================
// Navigation Tabs
// ===================================================================
function initNavigation() {
    initPortalSidebarToggle();

    const tabs = document.querySelectorAll('.nav-tab, .dropdown-menu-item');
    tabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            if (tab.id === 'navMoreBtn') return; // Handled separately
            const targetId = tab.getAttribute('data-target');
            if (targetId) switchTab(targetId);
        });
    });

    // More Dropdown Trigger & Accessibility
    const moreBtn = document.getElementById('navMoreBtn');
    const moreMenu = document.getElementById('navMoreMenu');
    if (moreBtn && moreMenu) {
        moreBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const isOpen = moreMenu.classList.toggle('show');
            moreBtn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        });

        // Close on click outside
        document.addEventListener('click', (e) => {
            if (!moreBtn.contains(e.target) && !moreMenu.contains(e.target)) {
                moreMenu.classList.remove('show');
                moreBtn.setAttribute('aria-expanded', 'false');
            }
        });

        // Close on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && moreMenu.classList.contains('show')) {
                moreMenu.classList.remove('show');
                moreBtn.setAttribute('aria-expanded', 'false');
                moreBtn.focus();
            }
        });
    }
}

function initPortalSidebarToggle() {
    const appSidebar = document.getElementById('appSidebar');
    const appWorkspace = document.getElementById('appWorkspace');
    const btnToggleSidebar = document.getElementById('btnToggleSidebar');

    if (!appSidebar) return;

    function setSidebarCollapsed(collapsed, save = true) {
        if (collapsed) {
            appSidebar.classList.add('collapsed');
            if (appWorkspace) appWorkspace.classList.add('sidebar-collapsed');
            if (btnToggleSidebar) {
                btnToggleSidebar.classList.remove('active');
                btnToggleSidebar.setAttribute('aria-expanded', 'false');
                btnToggleSidebar.setAttribute('title', 'Open Portal Sitemap (Ctrl+B)');
            }
        } else {
            appSidebar.classList.remove('collapsed');
            if (appWorkspace) appWorkspace.classList.remove('sidebar-collapsed');
            if (btnToggleSidebar) {
                btnToggleSidebar.classList.add('active');
                btnToggleSidebar.setAttribute('aria-expanded', 'true');
                btnToggleSidebar.setAttribute('title', 'Close Portal Sitemap (Ctrl+B)');
            }
        }
        if (save) {
            try {
                localStorage.setItem('agivant_portal_sitemap_collapsed', collapsed ? 'true' : 'false');
            } catch (err) {}
        }
    }

    function toggleSidebar() {
        const isCollapsed = appSidebar.classList.contains('collapsed');
        setSidebarCollapsed(!isCollapsed, true);
    }

    // Restore saved state
    try {
        const savedState = localStorage.getItem('agivant_portal_sitemap_collapsed');
        if (savedState === 'true') {
            setSidebarCollapsed(true, false);
        } else {
            setSidebarCollapsed(false, false);
        }
    } catch (err) {}

    if (btnToggleSidebar) {
        btnToggleSidebar.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            toggleSidebar();
        });
    }

    // Keyboard shortcut: Ctrl+B or Cmd+B
    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && (e.key === 'b' || e.key === 'B')) {
            const tag = (document.activeElement?.tagName || '').toLowerCase();
            if (tag !== 'input' && tag !== 'textarea') {
                e.preventDefault();
                toggleSidebar();
            }
        }
    });

    // Auto-collapse on narrow screens when a link is clicked
    const sitemapTabs = appSidebar.querySelectorAll('.nav-tab');
    sitemapTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            if (window.innerWidth <= 992) {
                setSidebarCollapsed(true, false);
            }
        });
    });
}

function switchTab(sectionId, options = {}) {
    // Guard admin-only sections
    const adminSections = ['analyticsSection', 'usersSection', 'auditLogsSection', 'processingJobsSection', 'systemSettingsSection'];
    if (adminSections.includes(sectionId) && AppState.currentUser?.role !== 'ROLE_ADMIN') {
        showAlert('Access Denied: This section requires Portal Administrator security clearance.', 'danger');
        return;
    }

    // Guard HR/Admin employee directory section
    if (sectionId === 'employeesSection' && !(AppState.currentUser?.role === 'ROLE_ADMIN' || AppState.currentUser?.role === 'ROLE_HR')) {
        showAlert('Access Denied: This section requires Administrator or HR security clearance.', 'danger');
        return;
    }

    // Clear search document state when leaving searchSection
    if (AppState.activeSection === 'searchSection' && sectionId !== 'searchSection') {
        if (typeof resetSearchSection === 'function') {
            resetSearchSection(false);
        }
    }

    // Reset AI Assistant to a fresh new chat whenever leaving it
    if (AppState.activeSection === 'aiAgentSection' && sectionId !== 'aiAgentSection') {
        if (typeof resetChatToNewState === 'function') {
            resetChatToNewState();
        }
    }

    // Reset document upload form when leaving documentsSection
    if (AppState.activeSection === 'documentsSection' && sectionId !== 'documentsSection') {
        if (typeof resetDocumentUploadForm === 'function') {
            resetDocumentUploadForm();
        }
    }

    document.querySelectorAll('.nav-tab, .dropdown-menu-item').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));

    const activeTab = document.querySelector(`[data-target="${sectionId}"]`);
    const activeSection = document.getElementById(sectionId);

    if (activeTab) activeTab.classList.add('active');
    if (activeSection) activeSection.classList.add('active');

    // Update More dropdown active style if an item inside is selected
    const moreBtn = document.getElementById('navMoreBtn');
    const isMoreItem = adminSections.includes(sectionId);
    if (moreBtn) {
        if (isMoreItem) {
            moreBtn.classList.add('active');
        } else {
            moreBtn.classList.remove('active');
        }
    }

    // Close More menu
    const moreMenu = document.getElementById('navMoreMenu');
    if (moreMenu) {
        moreMenu.classList.remove('show');
        if (moreBtn) moreBtn.setAttribute('aria-expanded', 'false');
    }

    AppState.activeSection = sectionId;
    if (sectionId === 'aiAgentSection') {
        // Always open a fresh new chat whenever navigating or returning to the AI Assistant tab
        if (typeof resetChatToNewState === 'function') {
            resetChatToNewState();
        }
        if (AppState.currentUser && AppState.currentUser.username) {
            if (activeChatUser !== AppState.currentUser.username || userConversations.length === 0) {
                loadUserChatHistory(AppState.currentUser.username);
            }
        }
        setTimeout(() => {
            const chatInput = document.getElementById('chatInputText');
            if (chatInput) {
                chatInput.value = '';
                chatInput.focus();
            }
        }, 60);
    } else if (sectionId === 'searchSection') {
        if (typeof resetSearchSection === 'function') {
            resetSearchSection(options && options.preserveSearchCategory === true);
        }
        setTimeout(() => {
            const searchInput = document.getElementById('searchInput');
            if (searchInput) searchInput.focus();
        }, 60);
    } else if (sectionId === 'documentsSection') {
        if (typeof resetDocumentUploadForm === 'function') {
            resetDocumentUploadForm();
        }
    }
    refreshActiveSection();
}

function refreshActiveSection() {
    switch (AppState.activeSection) {
        case 'dashboardSection':
            loadDashboardData();
            break;
        case 'searchSection':
            // Search section is freshly cleared and readied on navigation
            break;
        case 'documentsSection':
            loadDocumentsTable();
            break;
        case 'feedbackSection':
            loadFeedbackManagement();
            break;
        case 'employeesSection':
            loadEmployeesInformation();
            break;
        case 'analyticsSection':
            loadAnalyticsData();
            break;
        case 'usersSection':
            loadUsersDirectory();
            break;
        case 'auditLogsSection':
            loadAuditLogs();
            break;
        case 'processingJobsSection':
            loadProcessingJobs();
            break;
        case 'systemSettingsSection':
            loadSystemSettings();
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

    // 1. Employee Dashboard Quick Access Tiles
    const empActionSearch = document.getElementById('empActionSearch');
    if (empActionSearch) {
        empActionSearch.addEventListener('click', () => switchTab('searchSection'));
        empActionSearch.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                switchTab('searchSection');
            }
        });
    }

    const empActionAi = document.getElementById('empActionAi');
    if (empActionAi) {
        empActionAi.addEventListener('click', () => switchTab('aiAgentSection'));
        empActionAi.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                switchTab('aiAgentSection');
            }
        });
    }

    const empActionDocs = document.getElementById('empActionDocs');
    if (empActionDocs) {
        empActionDocs.addEventListener('click', () => switchTab('documentsSection'));
        empActionDocs.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                switchTab('documentsSection');
            }
        });
    }

    // 2. Employee Dashboard Category Shortcuts
    const browseBtns = document.querySelectorAll('.btn-browse-cat');
    browseBtns.forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            const cat = btn.getAttribute('data-category');
            navigateToCategory(cat, 'documentsSection');
        });
    });

    const searchLinks = document.querySelectorAll('.btn-search-cat');
    searchLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.stopPropagation();
            const cat = link.getAttribute('data-category');
            navigateToCategory(cat, 'searchSection');
        });
    });

    const catCards = document.querySelectorAll('.emp-cat-card');
    catCards.forEach(card => {
        card.addEventListener('click', (e) => {
            if (e.target.tagName !== 'BUTTON') {
                const cat = card.getAttribute('data-category');
                navigateToCategory(cat, 'documentsSection');
            }
        });
    });

    // 3. Admin Dashboard Clickable Summary Cards & Shortcuts
    const cardActive = document.getElementById('adminKpiCardActive');
    if (cardActive) {
        const handleActiveClick = () => {
            switchTab('documentsSection');
            const statusFilter = document.getElementById('docListStatusFilter');
            if (statusFilter) {
                statusFilter.value = 'ACTIVE';
                loadDocumentsTable();
            }
        };
        cardActive.addEventListener('click', handleActiveClick);
        cardActive.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                handleActiveClick();
            }
        });
    }

    const cardArchived = document.getElementById('adminKpiCardArchived');
    if (cardArchived) {
        const handleArchivedClick = () => {
            switchTab('documentsSection');
            const statusFilter = document.getElementById('docListStatusFilter');
            if (statusFilter) {
                statusFilter.value = 'ARCHIVED';
                loadDocumentsTable();
            }
        };
        cardArchived.addEventListener('click', handleArchivedClick);
        cardArchived.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                handleArchivedClick();
            }
        });
    }

    const cardAwaiting = document.getElementById('adminKpiCardAwaiting');
    if (cardAwaiting) {
        const handleAwaitingClick = () => {
            const healthCard = document.getElementById('adminProcessingHealthCard');
            if (healthCard) {
                healthCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
                healthCard.classList.add('highlight-pulse');
                setTimeout(() => healthCard.classList.remove('highlight-pulse'), 1500);
            }
        };
        cardAwaiting.addEventListener('click', handleAwaitingClick);
        cardAwaiting.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                handleAwaitingClick();
            }
        });
    }

    const cardRating = document.getElementById('adminKpiCardRating');
    if (cardRating) {
        const handleRatingClick = () => {
            switchTab('feedbackSection');
        };
        cardRating.addEventListener('click', handleRatingClick);
        cardRating.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                handleRatingClick();
            }
        });
    }

    const btnAdminViewAllDocs = document.getElementById('btnAdminViewAllDocs');
    if (btnAdminViewAllDocs) {
        btnAdminViewAllDocs.addEventListener('click', () => {
            switchTab('documentsSection');
        });
    }
}

function navigateToCategory(category, targetSection = 'documentsSection') {
    if (!category) return;

    if (targetSection === 'documentsSection') {
        const catFilter = document.getElementById('docListCategoryFilter');
        if (catFilter) {
            catFilter.value = category;
        }
        switchTab('documentsSection');
        loadDocumentsTable();
    } else if (targetSection === 'searchSection') {
        switchTab('searchSection', { preserveSearchCategory: true });
        const searchCatSelect = document.getElementById('searchCategorySelect');
        if (searchCatSelect) {
            searchCatSelect.value = category;
        }
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.focus();
        }
    }
}

async function loadDashboardData() {
    try {
        const res = await fetch('/api/knowledge/analytics');
        if (!res.ok) return;
        const data = await res.json();

        // 1. Update Administrator Dashboard KPIs
        const kpiActive = document.getElementById('kpiActiveDocs');
        if (kpiActive) kpiActive.textContent = (data.totalActiveDocuments || 0).toLocaleString();

        const kpiArchived = document.getElementById('kpiArchivedDocs');
        if (kpiArchived) kpiArchived.textContent = (data.totalArchivedDocuments || 0).toLocaleString();

        const kpiAwaiting = document.getElementById('kpiAwaitingReindex');
        if (kpiAwaiting) kpiAwaiting.textContent = (data.totalAwaitingReindex || 0).toLocaleString();

        const kpiRating = document.getElementById('kpiAvgRating');
        if (kpiRating) {
            if (data.hasFeedbackRatings) {
                const avg = Number(data.overallAverageRating || 0);
                kpiRating.textContent = `${avg.toFixed(1)} / 5`;
            } else {
                kpiRating.textContent = 'No ratings yet';
            }
        }

        // Update Employee Dashboard KPIs
        const empKpiActive = document.getElementById('empKpiActiveDocs');
        if (empKpiActive) empKpiActive.textContent = data.totalActiveDocuments || 0;

        const empKpiRating = document.getElementById('empKpiAvgRating');
        if (empKpiRating) {
            if (data.hasFeedbackRatings) {
                empKpiRating.textContent = (data.overallAverageRating || 0).toFixed(1);
            } else {
                empKpiRating.textContent = 'No ratings';
            }
        }

        // 2. Populate Knowledge Asset Breakdown Table
        const catTbody = document.getElementById('dashboardCategoryTbody');
        const catTfoot = document.getElementById('dashboardCategoryTfoot');
        const totalDocsBadge = document.getElementById('dashboardTotalDocsBadge');

        const catRows = data.documentsByCategoryAndStatus || [];
        let grandTotal = 0;
        let totalActive = 0;
        let totalArchived = 0;
        const catTotals = {};

        if (catTbody) {
            if (catRows.length === 0) {
                catTbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted">No documents registered in the knowledge base yet.</td></tr>';
                if (catTfoot) catTfoot.innerHTML = '';
                if (totalDocsBadge) totalDocsBadge.textContent = '0 Total Assets';
            } else {
                catTbody.innerHTML = catRows.map(item => {
                    const count = Number(item.count || 0);
                    grandTotal += count;
                    if (item.status === 'ACTIVE') totalActive += count;
                    if (item.status === 'ARCHIVED') totalArchived += count;

                    const catKey = item.category || 'OTHER';
                    catTotals[catKey] = (catTotals[catKey] || 0) + count;

                    const friendlyCategory = formatCategoryLabel(item.category);
                    const isStatusActive = String(item.status).toUpperCase() === 'ACTIVE';

                    return `
                        <tr>
                            <td><strong>${friendlyCategory}</strong></td>
                            <td class="text-center">
                                <span class="badge ${isStatusActive ? 'badge-active' : 'badge-archived'}">
                                    ${isStatusActive ? 'Active' : 'Archived'}
                                </span>
                            </td>
                            <td class="text-right"><strong>${count.toLocaleString()}</strong></td>
                        </tr>
                    `;
                }).join('');

                if (catTfoot) {
                    catTfoot.innerHTML = `
                        <tr>
                            <td><strong>Total Assets</strong></td>
                            <td class="text-center">
                                <span style="font-size: 0.8rem; color: var(--text-secondary);">
                                    ${totalActive} Active &bull; ${totalArchived} Archived
                                </span>
                            </td>
                            <td class="text-right"><strong>${grandTotal.toLocaleString()}</strong></td>
                        </tr>
                    `;
                }

                if (totalDocsBadge) {
                    totalDocsBadge.textContent = `${grandTotal} Total Assets`;
                }
            }
        }

        // 3. Render Visual Category Distribution Bar
        renderCategoryDistribution(catTotals, grandTotal);

        // 4. Populate Frequently Searched Categories Table
        const searchTbody = document.getElementById('dashboardSearchTbody');
        const searchBadge = document.getElementById('dashboardSearchInquiriesBadge');
        const searchRows = data.frequentlySearchedCategories || [];

        if (searchTbody) {
            if (searchRows.length === 0) {
                searchTbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted">No search inquiries recorded yet.</td></tr>';
                if (searchBadge) searchBadge.textContent = '0 Inquiries';
            } else {
                let totalInquiries = 0;
                searchTbody.innerHTML = searchRows.map(item => {
                    const searchCount = Number(item.searchCount || 0);
                    totalInquiries += searchCount;
                    const avgResults = Number(item.avgResults || 0);
                    const friendlyCategory = formatSearchCategoryLabel(item.category);

                    return `
                        <tr>
                            <td><strong>${friendlyCategory}</strong></td>
                            <td class="text-center">${searchCount.toLocaleString()}</td>
                            <td class="text-right"><strong>${avgResults.toFixed(1)}</strong></td>
                        </tr>
                    `;
                }).join('');

                if (searchBadge) {
                    searchBadge.textContent = `${totalInquiries.toLocaleString()} Total Inquiries`;
                }
            }
        }

        // 5. Populate Document Processing Health Section
        renderProcessingHealth(data);

        // 6. Populate Recent Administrative Activity Section
        loadRecentAdministrativeActivity();

    } catch (err) {
        console.error('Failed to load dashboard data:', err);
    }
}

// Category Distribution Bar Rendering Helper
function renderCategoryDistribution(catTotals, grandTotal) {
    const container = document.getElementById('categoryDistributionContainer');
    const barEl = document.getElementById('categoryDistributionBar');
    const legendEl = document.getElementById('categoryDistributionLegend');
    const ratioText = document.getElementById('distributionRatioText');

    if (!container || !barEl || !legendEl) return;

    if (!grandTotal || grandTotal <= 0) {
        container.style.display = 'none';
        return;
    }

    container.style.display = 'block';

    const categories = [
        { key: 'TECHNICAL_RUNBOOK', label: 'Technical Guides', cssClass: 'dist-technical', dotClass: 'dot-technical' },
        { key: 'POLICY', label: 'Policies', cssClass: 'dist-policy', dotClass: 'dot-policy' },
        { key: 'FAQ', label: 'Common Questions', cssClass: 'dist-faq', dotClass: 'dot-faq' },
        { key: 'OTHER', label: 'Other', cssClass: 'dist-other', dotClass: 'dot-other' }
    ];

    let segmentsHtml = '';
    let legendHtml = '';
    let activeCategoryCount = 0;

    categories.forEach(c => {
        const count = catTotals[c.key] || 0;
        if (count > 0) {
            activeCategoryCount++;
            const pct = Math.round((count / grandTotal) * 100);
            segmentsHtml += `<div class="dist-segment ${c.cssClass}" style="width: ${pct}%;" title="${c.label}: ${count} docs (${pct}%)"></div>`;
            legendHtml += `
                <span class="legend-item">
                    <span class="legend-dot ${c.dotClass}"></span>
                    <span>${c.label}: <strong>${count}</strong> (${pct}%)</span>
                </span>
            `;
        }
    });

    barEl.innerHTML = segmentsHtml;
    legendEl.innerHTML = legendHtml;
    if (ratioText) {
        ratioText.textContent = `${grandTotal} documents across ${activeCategoryCount} categories`;
    }
}

// Document Processing Health Helper
function renderProcessingHealth(data) {
    const badgeEl = document.getElementById('healthStatusBadge');
    const bodyEl = document.getElementById('healthCardBody');
    if (!bodyEl) return;

    const health = data.processingHealth || {};
    const awaitingDocs = data.documentsAwaitingReindex || [];
    const awaitingCount = data.totalAwaitingReindex || awaitingDocs.length || 0;
    const failedCount = data.totalFailedIndexing || (health.failedJobs || 0);
    const activeCount = data.totalActiveDocuments || 0;
    const lastSuccessTimeStr = health.lastSuccessTime ? new Date(health.lastSuccessTime).toLocaleString() : 'Recently';

    if (failedCount > 0) {
        if (badgeEl) {
            badgeEl.className = 'badge badge-admin';
            badgeEl.textContent = `❌ ${failedCount} Failed Jobs`;
        }
        bodyEl.innerHTML = `
            <div class="health-status-box" style="border-left: 4px solid #ef4444; background: #fef2f2; padding: 1.25rem; border-radius: 6px;">
                <div style="display: flex; gap: 0.85rem; align-items: flex-start;">
                    <div style="font-size: 1.8rem;">⚠️</div>
                    <div style="flex: 1;">
                        <h4 style="margin: 0 0 0.35rem 0; color: #b91c1c;">Document Processing Alert (Degraded)</h4>
                        <p style="margin: 0 0 0.65rem 0; font-size: 0.88rem; color: #7f1d1d; line-height: 1.5;">
                            ${failedCount} document processing pipeline execution(s) encountered parsing or vectorization errors. Keyword retrieval fallback is active.
                        </p>
                        <div class="health-meta-tags" style="display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.85rem;">
                            <span class="meta-tag" style="background: #fee2e2; color: #991b1b; font-weight: 600;">⚡ Status: DEGRADED</span>
                            <span class="meta-tag">📄 Synced: ${activeCount}</span>
                            <span class="meta-tag">🔍 Search: Available (Keyword Fallback)</span>
                            <span class="meta-tag">🕒 Last Success: ${lastSuccessTimeStr}</span>
                        </div>
                        <button type="button" class="btn btn-primary btn-sm" onclick="switchTab('processingJobsSection')">
                            Inspect &amp; Retry Jobs in Processing Queue &rarr;
                        </button>
                    </div>
                </div>
            </div>
        `;
    } else if (awaitingCount === 0) {
        if (badgeEl) {
            badgeEl.className = 'badge badge-success';
            badgeEl.textContent = '✓ Operational';
        }
        bodyEl.innerHTML = `
            <div class="health-status-box healthy">
                <div class="health-icon">✅</div>
                <div class="health-info">
                    <h4>All Knowledge Assets Fully Synchronized</h4>
                    <p>All active documents are indexed and segmented into searchable passages. Zero pipeline errors or pending jobs.</p>
                    <div class="health-meta-tags">
                        <span class="meta-tag">⚡ Pipeline: Operational</span>
                        <span class="meta-tag">📄 Synced Documents: ${activeCount}</span>
                        <span class="meta-tag">🔍 Search Engine: Keyword Scoring Fallback</span>
                        <span class="meta-tag">🕒 Last Success: ${lastSuccessTimeStr}</span>
                    </div>
                </div>
            </div>
        `;
    } else {
        if (badgeEl) {
            badgeEl.className = 'badge badge-warning';
            badgeEl.textContent = `⚠️ ${awaitingCount} Awaiting Re-index`;
        }
        bodyEl.innerHTML = `
            <div class="health-status-box warning">
                <div class="health-icon">⚠️</div>
                <div class="health-info">
                    <h4>${awaitingCount} Document(s) Require Vector Synchronization</h4>
                    <p>The following documents were updated and require re-segmentation to refresh search passages:</p>
                    <div class="reindex-list">
                        ${awaitingDocs.map(doc => `
                            <div class="reindex-item">
                                <div class="reindex-item-info">
                                    <strong>${escapeHtml(doc.title)}</strong>
                                    <span style="color: var(--text-secondary); margin-left: 0.35rem;">
                                        (v${doc.version} &bull; ${formatCategoryLabel(doc.category)})
                                    </span>
                                </div>
                                <button type="button" class="btn btn-secondary btn-sm btn-reindex-health" data-doc-id="${doc.id}">
                                    🔄 Re-index Now
                                </button>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;

        bodyEl.querySelectorAll('.btn-reindex-health').forEach(btn => {
            btn.addEventListener('click', async () => {
                const docId = btn.getAttribute('data-doc-id');
                if (docId) {
                    await reindexDocument(docId);
                    loadDashboardData();
                }
            });
        });
    }
}

// Recent Administrative Activity Helper
async function loadRecentAdministrativeActivity() {
    const bodyEl = document.getElementById('recentActivityBody');
    if (!bodyEl) return;

    try {
        const res = await fetch('/api/knowledge/documents');
        if (!res.ok) {
            bodyEl.innerHTML = '<p class="text-muted">Unable to retrieve recent document activity.</p>';
            return;
        }

        const docs = await res.json();
        if (!Array.isArray(docs) || docs.length === 0) {
            bodyEl.innerHTML = '<p class="text-muted">No document activity recorded in the repository yet.</p>';
            return;
        }

        // Sort by updatedAt descending
        const sorted = docs.slice().sort((a, b) => {
            const timeA = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
            const timeB = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
            return timeB - timeA;
        });

        const top5 = sorted.slice(0, 5);

        bodyEl.innerHTML = `
            <div class="activity-timeline">
                ${top5.map(doc => {
                    const isActive = doc.status === 'ACTIVE';
                    const icon = isActive ? '📄' : '📦';
                    const iconClass = isActive ? 'active-icon' : 'archived-icon';
                    const friendlyCat = formatCategoryLabel(doc.category);
                    const owner = formatOwnerName(doc.ownerUsername);
                    const timeStr = formatActivityTime(doc.updatedAt || doc.createdAt);

                    return `
                        <div class="activity-item">
                            <div class="activity-icon ${iconClass}">
                                ${icon}
                            </div>
                            <div class="activity-content">
                                <div class="activity-title-row">
                                    <span class="activity-title" data-doc-id="${doc.id}" style="cursor: pointer;" title="Click to view full document">
                                        ${escapeHtml(doc.title)}
                                    </span>
                                    <span class="badge ${isActive ? 'badge-active' : 'badge-archived'}">
                                        ${isActive ? 'Active' : 'Archived'}
                                    </span>
                                </div>
                                <div class="activity-meta">
                                    <span>v${doc.version}</span>
                                    <span>&bull;</span>
                                    <span>${friendlyCat}</span>
                                    <span>&bull;</span>
                                    <span>Updated by ${escapeHtml(owner)}</span>
                                    <span>&bull;</span>
                                    <span>${timeStr}</span>
                                </div>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;

        bodyEl.querySelectorAll('.activity-title[data-doc-id]').forEach(el => {
            el.addEventListener('click', () => {
                const id = el.getAttribute('data-doc-id');
                if (id) viewDocumentDetails(id);
            });
        });

    } catch (err) {
        console.error('Failed to load recent activity:', err);
        bodyEl.innerHTML = '<p class="text-muted">Failed to load recent activity.</p>';
    }
}

function formatActivityTime(dateStr) {
    if (!dateStr) return 'Recently';
    try {
        const d = new Date(dateStr);
        if (isNaN(d.getTime())) return dateStr;
        return d.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });
    } catch {
        return dateStr;
    }
}

// Category Display Formatting Helper
function formatCategoryLabel(cat, customCat) {
    if (customCat && String(customCat).trim() !== '') {
        return escapeHtml(customCat.trim());
    }
    if (!cat) return 'Uncategorized';
    switch (String(cat).toUpperCase()) {
        case 'POLICY': return 'Policies';
        case 'TECHNICAL_RUNBOOK': return 'Technical Guides';
        case 'FAQ': return 'Common Questions';
        case 'OTHER': return 'Other';
        case 'ALL':
        case 'ALL_CATEGORIES': return 'All Categories';
        default: return escapeHtml(cat || '');
    }
}

function formatSearchCategoryLabel(cat) {
    if (!cat || cat === 'ALL' || cat === 'ALL_CATEGORIES') {
        return 'All Categories';
    }
    return formatCategoryLabel(cat);
}

// Friendly Status Label Formatting Helper
function formatStatusLabel(status) {
    if (!status) return 'Active';
    const s = String(status).toUpperCase();
    if (s === 'ACTIVE') return 'Active';
    if (s === 'ARCHIVED') return 'Archived';
    return status;
}

// Friendly Access Clearance Label Formatting Helper
function formatAccessLevelLabel(access) {
    if (!access) return 'Employee Access';
    const a = String(access).toUpperCase();
    if (a === 'EMPLOYEE') return 'Employee Access';
    if (a === 'PUBLIC') return 'Public Access';
    if (a === 'HR') return 'HR Access';
    if (a === 'ADMIN') return 'Admin Access';
    return access;
}

// Verified Friendly User & Role Mapping Helper
function formatUserWithRole(creator) {
    if (!creator) return 'System';
    const trimmed = String(creator).trim();
    if (trimmed.includes('(') && trimmed.includes(')')) return trimmed;
    const lower = trimmed.toLowerCase();
    if (lower === 'admin_user') return 'Ashwin Mani (Administrator)';
    if (lower === 'hr_user') return 'Suganthi Mani (HR Manager)';
    if (lower === 'aswini') return 'Aswini Mani (Senior Software Engineer)';
    if (lower === 'employee1') return 'Alice Employee (Software Engineer)';
    return trimmed;
}

function formatOwnerName(rawOwner) {
    if (!rawOwner) return 'Internal System';
    return formatUserWithRole(rawOwner);
}

// Structured Text Document Formatter
function formatParagraphsAndLists(rawText) {
    if (!rawText) return '';
    const rawParagraphs = rawText.split(/\n\s*\n/);
    let html = '';

    for (const para of rawParagraphs) {
        const trimmed = para.trim();
        if (!trimmed) continue;

        const lines = trimmed.split('\n').map(l => l.trim()).filter(l => l.length > 0);

        const isBulletList = lines.length > 1 && lines.every(l => /^[-*•]\s+/.test(l));
        const isNumberedList = lines.length > 1 && lines.every(l => /^\d+[\.\)]\s+/.test(l));

        if (isBulletList) {
            html += '<ul class="doc-list">';
            for (const line of lines) {
                const itemText = line.replace(/^[-*•]\s+/, '');
                html += `<li class="doc-list-item">${escapeHtml(itemText)}</li>`;
            }
            html += '</ul>';
        } else if (isNumberedList) {
            html += '<ol class="doc-list">';
            for (const line of lines) {
                const itemText = line.replace(/^\d+[\.\)]\s+/, '');
                html += `<li class="doc-list-item">${escapeHtml(itemText)}</li>`;
            }
            html += '</ol>';
        } else if (lines.length === 1 && lines[0].length < 80 && (lines[0].endsWith(':') || /^#{1,3}\s+|^Section\s+\d+|^Chapter\s+\d+/i.test(lines[0]))) {
            const headingText = lines[0].replace(/^#{1,3}\s+/, '').replace(/:$/, '');
            html += `<h4 class="doc-section-title" style="margin-top: 1.25rem; font-size: 1.02rem;">${escapeHtml(headingText)}</h4>`;
        } else {
            html += `<p class="doc-paragraph">${escapeHtml(trimmed)}</p>`;
        }
    }

    return html;
}

function renderStructuredDocumentContent(fullText, chunks, doc) {
    // 1. Check if genuine section references exist in chunks
    const validSections = (chunks || []).filter(c => {
        if (!c.sectionReference) return false;
        const ref = c.sectionReference.trim();
        return ref !== '' && 
               ref !== 'General Section' && 
               !/^chunk\s*\d+/i.test(ref);
    });

    if (validSections.length > 0) {
        const sectionMap = new Map();
        validSections.forEach(c => {
            const secTitle = c.sectionReference.trim();
            if (!sectionMap.has(secTitle)) {
                sectionMap.set(secTitle, []);
            }
            if (c.content && c.content.trim()) {
                sectionMap.get(secTitle).push(c.content.trim());
            }
        });

        let html = '<div class="structured-doc-view">';
        sectionMap.forEach((contents, secTitle) => {
            html += `<div class="doc-section-block">`;
            html += `<h3 class="doc-section-title">📄 ${escapeHtml(secTitle)}</h3>`;
            contents.forEach(content => {
                html += formatParagraphsAndLists(content);
            });
            html += `</div>`;
        });
        html += '</div>';
        return html;
    }

    // 2. Fall back to formatting complete continuous text
    const textToFormat = (fullText && fullText.trim()) ? fullText.trim() : (doc.description || '');
    if (!textToFormat) {
        return `
            <div class="structured-doc-view" style="text-align: center; padding: 2.5rem; color: var(--text-muted);">
                <p>Document content is currently unavailable.</p>
            </div>
        `;
    }

    let html = '<div class="structured-doc-view">';
    html += '<div class="doc-notice-banner"><span>ℹ️</span> Complete document text presented in standard reading view.</div>';
    html += formatParagraphsAndLists(textToFormat);
    html += '</div>';
    return html;
}

// Excerpt formatting helper: avoids truncated partial words, prefers complete sentences, uses proper ellipsis
function formatExcerpt(rawSnippet) {
    if (!rawSnippet) return '';
    let text = rawSnippet.trim();

    // Detect if input was already truncated with ellipsis
    const hasLeadingEllipsis = text.startsWith('...') || text.startsWith('…');
    const hasTrailingEllipsis = text.endsWith('...') || text.endsWith('…');

    // Strip existing ellipses and spaces
    text = text.replace(/^(\.\.\.|…)\s*/, '').replace(/\s*(\.\.\.|…)$/, '').trim();
    if (!text) return '';

    // Check for complete sentence(s) (starting with capital letter and ending with punctuation)
    const sentenceMatches = text.match(/[A-Z][^.!?]*[.!?]/g);
    if (sentenceMatches && sentenceMatches.length > 0) {
        const fullSentences = sentenceMatches.join(' ').trim();
        if (fullSentences.length >= 25) {
            return fullSentences;
        }
    }

    // Otherwise, ensure word boundaries are clean
    let words = text.split(/\s+/);

    // If truncated at start, drop broken leading word fragment if it started lowercase
    if (hasLeadingEllipsis && words.length > 1) {
        if (/^[a-z]/.test(words[0])) {
            words.shift();
        }
    }

    // If truncated at end, drop trailing word fragment if it didn't end with sentence punctuation
    if (hasTrailingEllipsis && words.length > 1) {
        if (!/[.!?]$/.test(words[words.length - 1])) {
            words.pop();
        }
    }

    let cleaned = words.join(' ').trim();
    if (!cleaned) return rawSnippet;

    const prefix = hasLeadingEllipsis ? '…' : '';
    const suffix = hasTrailingEllipsis && !/[.!?]$/.test(cleaned) ? '…' : '';

    return `${prefix}${cleaned}${suffix}`;
}

// Groups search results under distinct document cards to reduce visual repetition
function groupSearchResults(results) {
    const grouped = [];
    const docMap = new Map();

    let maxScore = 0;
    results.forEach(item => {
        if (item.score > maxScore) maxScore = item.score;
    });

    results.forEach(item => {
        const key = item.documentId;
        const normalizedTitle = (item.title || '').trim().toLowerCase().replace(/\s+v\d+$/i, '');

        let existingGroup = docMap.get(key);
        if (!existingGroup) {
            existingGroup = grouped.find(g => (g.title || '').trim().toLowerCase().replace(/\s+v\d+$/i, '') === normalizedTitle);
        }

        if (!existingGroup) {
            const isPdf = (item.contentType && item.contentType.toLowerCase().includes('pdf')) ||
                          (item.originalFileName && item.originalFileName.toLowerCase().endsWith('.pdf'));
            const fileType = isPdf ? 'PDF' : (item.contentType && item.contentType.toLowerCase().includes('text') || (item.originalFileName && item.originalFileName.toLowerCase().endsWith('.txt')) ? 'TXT' : (item.originalFileName ? item.originalFileName.split('.').pop().toUpperCase() : 'TXT'));

            const docGroup = {
                documentId: item.documentId,
                title: item.title,
                category: item.category,
                version: item.version,
                description: item.description || '',
                contentType: item.contentType || '',
                originalFileName: item.originalFileName || '',
                fileType: fileType,
                maxScore: item.score,
                isTopMatch: (item.score === maxScore && maxScore > 0),
                passages: []
            };
            docMap.set(key, docGroup);
            grouped.push(docGroup);
            existingGroup = docGroup;
        } else {
            if (item.score > existingGroup.maxScore) {
                existingGroup.maxScore = item.score;
            }
            if (item.score === maxScore && maxScore > 0) {
                existingGroup.isTopMatch = true;
            }
            if (!existingGroup.description && item.description) {
                existingGroup.description = item.description;
            }
            if (!existingGroup.contentType && item.contentType) {
                existingGroup.contentType = item.contentType;
            }
            if (!existingGroup.originalFileName && item.originalFileName) {
                existingGroup.originalFileName = item.originalFileName;
            }
        }
        existingGroup.passages.push(item);
    });

    return grouped;
}

// ===================================================================
// SCREEN B: SEARCH DOCUMENTS
// ===================================================================
function resetSearchSection(preserveCategory = false) {
    const searchInput = document.getElementById('searchInput');
    const categorySelect = document.getElementById('searchCategorySelect');
    const statusSelect = document.getElementById('searchStatusSelect');
    const resultsList = document.getElementById('searchResultsList');
    const metaBar = document.getElementById('searchMetaBar');
    const countText = document.getElementById('searchResultCountText');
    const loading = document.getElementById('searchLoading');
    const clearBtn = document.getElementById('searchClearInputBtn');

    if (searchInput) {
        searchInput.value = '';
    }
    if (clearBtn) {
        clearBtn.style.display = 'none';
    }
    if (!preserveCategory && categorySelect) {
        categorySelect.value = '';
    }
    if (statusSelect) {
        statusSelect.value = 'ACTIVE';
    }
    if (resultsList) {
        resultsList.innerHTML = '';
    }
    if (metaBar) {
        metaBar.style.display = 'none';
    }
    if (countText) {
        countText.textContent = '0 matching documents found';
    }
    if (loading) {
        loading.style.display = 'none';
    }
}

function initSearch() {
    const form = document.getElementById('searchForm');
    const searchInput = document.getElementById('searchInput');
    const clearBtn = document.getElementById('searchClearInputBtn');
    const resetBtn = document.getElementById('searchResetBtn');

    if (searchInput) {
        searchInput.addEventListener('input', () => {
            const hasText = searchInput.value.trim().length > 0;
            if (clearBtn) {
                clearBtn.style.display = searchInput.value.length > 0 ? 'inline-flex' : 'none';
            }
            if (!hasText) {
                const resultsList = document.getElementById('searchResultsList');
                const metaBar = document.getElementById('searchMetaBar');
                if (resultsList) resultsList.innerHTML = '';
                if (metaBar) metaBar.style.display = 'none';
            }
        });
    }

    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            if (searchInput) {
                searchInput.value = '';
                searchInput.focus();
            }
            clearBtn.style.display = 'none';
            const resultsList = document.getElementById('searchResultsList');
            const metaBar = document.getElementById('searchMetaBar');
            if (resultsList) resultsList.innerHTML = '';
            if (metaBar) metaBar.style.display = 'none';
        });
    }

    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            resetSearchSection(false);
            if (searchInput) searchInput.focus();
        });
    }

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
                    const err = await res.json().catch(() => ({}));
                    showAlert(err.message || "We couldn't retrieve documents right now. Please try again.", 'danger');
                    return;
                }

                const data = await res.json();
                metaBar.style.display = 'block';

                const groupedDocs = groupSearchResults(data.results);

                if (data.results.length === 0) {
                    countText.textContent = `0 matching documents found for "${escapeHtml(data.query)}"`;
                    resultsList.innerHTML = `
                        <div class="card" style="padding: 2rem; text-align: center;">
                            <p style="font-size: 1.1rem; color: var(--text-secondary);">No matching documents found. Try different keywords.</p>
                        </div>
                    `;
                } else {
                    countText.textContent = `${groupedDocs.length} matching document(s) found for "${escapeHtml(data.query)}"`;

                    resultsList.innerHTML = groupedDocs.map(docGroup => {
                        const bestPassage = docGroup.passages[0];
                        const excerpt = formatExcerpt(bestPassage.snippet);

                        return `
                            <div class="search-result-card">
                                <div class="search-result-header">
                                    <div style="flex: 1; min-width: 0;">
                                        <div style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
                                            <span class="search-result-title">${escapeHtml(docGroup.title)}</span>
                                            ${docGroup.fileType ? `<span class="badge ${docGroup.fileType === 'PDF' ? 'badge-admin' : 'badge-mode'}">${escapeHtml(docGroup.fileType)}</span>` : ''}
                                        </div>
                                        <div class="search-result-meta" style="margin-top: 0.35rem; margin-bottom: 0;">
                                            <span><strong>Category:</strong> ${formatCategoryLabel(docGroup.category)}</span>
                                            <span><strong>Version:</strong> v${docGroup.version}</span>
                                            ${docGroup.fileType ? `<span><strong>File Type:</strong> ${escapeHtml(docGroup.fileType)}</span>` : ''}
                                            ${docGroup.passages.length > 1 ? `<span><strong>Matching Sections:</strong> ${docGroup.passages.length}</span>` : ''}
                                        </div>
                                    </div>
                                    ${docGroup.isTopMatch ? '<span class="badge badge-success" style="font-size: 0.78rem; font-weight: 700; padding: 0.3rem 0.65rem;">Best match</span>' : ''}
                                </div>

                                ${docGroup.description ? `
                                    <p class="search-doc-desc" style="font-size: 0.88rem; color: var(--text-secondary); margin: 0.65rem 0 0.4rem 0; line-height: 1.5;">
                                        ${escapeHtml(docGroup.description)}
                                    </p>
                                ` : ''}

                                <div class="search-snippet" style="margin-top: 0.65rem;">
                                    <span style="font-size: 0.8rem; font-weight: 600; color: var(--primary); display: block; margin-bottom: 0.25rem;">
                                        📍 Relevant excerpt (${escapeHtml(bestPassage.sectionReference || 'Section ' + bestPassage.chunkIndex)}):
                                    </span>
                                    "${escapeHtml(excerpt)}"
                                </div>

                                <div style="margin-top: 0.85rem; display: flex; justify-content: flex-end; align-items: center; gap: 0.75rem;">
                                    <button type="button" class="btn btn-primary btn-sm" onclick="openFullDocumentReader(${docGroup.documentId})">
                                        📖 Read Full Document
                                    </button>
                                </div>
                            </div>
                        `;
                    }).join('');
                }
            } catch (err) {
                loading.style.display = 'none';
                console.error('Search error:', err);
                showAlert("We couldn't retrieve documents right now. Please try again.", 'danger');
            }
        });
    }
}

// ===================================================================
// SCREEN C: AI RESEARCH AGENT Q&A
// ===================================================================
let currentAiResponseData = null;
let currentAiQuestion = '';

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

function formatMarkdownSpans(text) {
    if (!text) return '';
    let formatted = escapeHtml(text);
    // Code blocks / inline code
    formatted = formatted.replace(/`([^`]+)`/g, '<code>$1</code>');
    // Bold: **text** or __text__
    formatted = formatted.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    formatted = formatted.replace(/__([^_]+)__/g, '<strong>$1</strong>');
    // Italics: *text* or _text_
    formatted = formatted.replace(/\*([^*]+)\*/g, '<em>$1</em>');
    formatted = formatted.replace(/_([^_]+)_/g, '<em>$1</em>');
    return formatted;
}

function formatRichAiAnswer(answerText) {
    if (!answerText) return '<p class="text-muted">No response content generated.</p>';

    const rawLines = answerText.split('\n');
    let html = '';
    let inList = false;

    for (let i = 0; i < rawLines.length; i++) {
        let line = rawLines[i].trim();
        if (!line) {
            if (inList) {
                html += '</ul>';
                inList = false;
            }
            continue;
        }

        // Clean out noisy dot trails
        line = line.replace(/\.{3,}/g, '');

        // Check if line is a document citation header
        const docHeaderMatch = line.match(/^(?:(?:\d+[\.\)])\s+)?According to ['"]?([^'":]+)['"]?:?$/i);
        if (docHeaderMatch) {
            if (inList) {
                html += '</ul>';
                inList = false;
            }
            const docName = docHeaderMatch[1].trim();
            html += `
                <div class="answer-doc-header" style="margin: 0.85rem 0 0.5rem; font-weight: 600; color: var(--primary);">
                    <span>📄 Document Source: <strong>${escapeHtml(docName)}</strong></span>
                </div>
            `;
            continue;
        }

        // Check for multi-doc synthesis announcement
        if (line.toLowerCase().startsWith('synthesizing information across')) {
            if (inList) {
                html += '</ul>';
                inList = false;
            }
            html += `
                <div style="font-weight: 600; color: var(--text-secondary); margin-bottom: 0.75rem;">
                    🌐 ${escapeHtml(line)}
                </div>
            `;
            continue;
        }

        // Check for numbered items (e.g., 1. or 1))
        const numMatch = line.match(/^(\d+)[\.\)]\s+(.*)$/);
        // Check for bullet items (e.g., *, -, •)
        const bulletMatch = line.match(/^[•\-\*]\s+(.*)$/);

        if (bulletMatch) {
            if (!inList) {
                html += '<ul class="answer-bullet-list" style="margin: 0.5rem 0 0.85rem 1.25rem; padding-left: 0.5rem; line-height: 1.7;">';
                inList = true;
            }
            const bulletBody = bulletMatch[1].trim();
            html += `<li style="margin-bottom: 0.4rem;">${formatMarkdownSpans(bulletBody)}</li>`;
        } else if (numMatch) {
            if (inList) {
                html += '</ul>';
                inList = false;
            }
            const num = numMatch[1];
            const numBody = numMatch[2].trim();
            html += `
                <div class="answer-step-item" style="display: flex; gap: 0.6rem; align-items: baseline; margin-bottom: 0.5rem;">
                    <span class="step-num-badge" style="background: var(--primary-light, #e0e7ff); color: var(--primary, #3b82f6); font-weight: 700; border-radius: 50%; min-width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center; font-size: 0.8rem;">${num}</span>
                    <div class="step-content" style="flex: 1; line-height: 1.6;">${formatMarkdownSpans(numBody)}</div>
                </div>
            `;
        } else {
            if (inList) {
                html += '</ul>';
                inList = false;
            }
            html += `<p style="margin-bottom: 0.75rem; line-height: 1.65; color: var(--text-primary);">${formatMarkdownSpans(line)}</p>`;
        }
    }

    if (inList) {
        html += '</ul>';
    }

    return html;
}

// ===================================================================
// SCREEN C: AI RESEARCH AGENT CONVERSATIONAL CHATBOT VIEW
// ===================================================================
let activeChatUser = null;
let isChatSubmitting = false;
let userConversations = [];
let userConversationTurns = [];
let activeConversationId = null;

function formatChatTime(dateInput) {
    if (!dateInput) return 'Just now';
    try {
        const d = new Date(dateInput);
        if (isNaN(d.getTime())) return 'Just now';
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
        return 'Just now';
    }
}

function formatChatDateShort(dateInput) {
    if (!dateInput) return '';
    try {
        const d = new Date(dateInput);
        if (isNaN(d.getTime())) return '';
        const today = new Date();
        const isToday = d.toDateString() === today.toDateString();
        if (isToday) {
            return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        }
        return d.toLocaleDateString([], { month: 'short', day: 'numeric' }) + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
        return '';
    }
}

function updateUserSitemapBar(user) {
    const sitemapAvatar = document.getElementById('sitemapUserAvatar');
    const sitemapName = document.getElementById('sitemapUserName');
    const sitemapRole = document.getElementById('sitemapUserRole');

    const displayName = user?.fullName || activeChatUser || 'User';
    const roleName = user?.role || 'EMPLOYEE';
    const initials = displayName
        .split(' ')
        .map(n => n.charAt(0))
        .join('')
        .substring(0, 2)
        .toUpperCase() || 'U';

    if (sitemapName) sitemapName.textContent = displayName;
    if (sitemapRole) sitemapRole.textContent = roleName;
    if (sitemapAvatar) sitemapAvatar.textContent = initials;
}

/**
 * Generates an intelligent, clean summarization name based on the first chat in the conversation.
 */
function summarizeConversationTitle(query, secondaryQuery = '') {
    if (!query || typeof query !== 'string') return 'New Conversation';
    let raw = query.trim();
    if (!raw) return 'New Conversation';

    // If first query is just a generic greeting ("hi", "hello", "hey"), check if secondary substantive query exists
    const isGreeting = /^(hi|hello|hey|greetings|good\s+(morning|afternoon|evening)|test|ping)(\s+there)?[\.!\?]*$/i.test(raw);
    if (isGreeting && secondaryQuery && secondaryQuery.trim()) {
        raw = secondaryQuery.trim();
    }

    const lower = raw.toLowerCase();

    // Domain recognizers for instant accurate titles
    if (lower.includes('agivant') && (lower.includes('hyderabad') || lower.includes('location') || lower.includes('office') || lower.includes('present in'))) {
        return 'Agivant Hyderabad Office';
    }
    if (lower.includes('agivant') && (lower.includes('about') || lower.includes('company') || lower.includes('overview') || lower.includes('who is') || lower.includes('what is agivant'))) {
        return 'Agivant Corporate Overview';
    }
    if (lower.includes('carry-over') || lower.includes('carry over')) {
        return 'Annual Leave Carry-Over Policy';
    }
    if (lower.includes('leave') && (lower.includes('quota') || lower.includes('balance') || lower.includes('how many leaves') || lower.includes('my leave'))) {
        return 'Employee Leave Quota & Balance';
    }
    if (lower.includes('remote work') || lower.includes('monitor') || lower.includes('allowance') || lower.includes('stipend')) {
        return 'Remote Work Allowance Policy';
    }
    if (lower.includes('technical guide') || lower.includes('repository guide') || lower.includes('how many technical')) {
        return 'Technical Documentation Guides';
    }
    if (lower.includes('bolt') || lower.includes('hardware')) {
        return 'Bolt Technical Specification';
    }

    if (isGreeting) {
        return 'General AI Inquiry';
    }

    // Strip preamble and question words
    let clean = raw.replace(/^[\s\W_]+/, '');
    const leadingFillers = [
        /^(please\s+)?(can|could|would)\s+you\s+(please\s+)?(tell|explain|show|give|provide)\s+(me\s+)?(about\s+)?/i,
        /^(please\s+)?tell\s+me\s+(about\s+)?/i,
        /^(please\s+)?give\s+me\s+(details|information|info)?(\s+about)?\s+/i,
        /^(what\s+is|what\s+are|what's)\s+(the\s+)?/i,
        /^(how\s+many|how\s+much|how\s+do|how\s+can|how\s+to)\s+/i,
        /^(is\s+there|are\s+there|is\s+it|is\s+agivant)\s+/i,
        /^(in\s+which\s+location\s+is\s+there|which\s+location\s+is\s+there)\s*/i,
        /^(do\s+we\s+have|does\s+company\s+have)\s+/i,
        /^(i\s+want\s+to\s+know\s+about|i\s+need\s+to\s+know\s+about)\s+/i,
        /^(summarize|explain)\s+(it\s+)?(in\s+simple\s+)?(some\s+more\s+)?/i
    ];

    for (const pattern of leadingFillers) {
        clean = clean.replace(pattern, '').trim();
    }

    // Remove trailing punctuation
    clean = clean.replace(/[\?\.\!]+$/, '').trim();

    if (!clean || clean.length < 2) {
        clean = raw.replace(/[\?\.\!]+$/, '').trim();
    }

    // Title Case formatting
    clean = clean
        .split(/\s+/)
        .map((w, idx) => {
            const lw = w.toLowerCase();
            const smallWords = ['in', 'on', 'at', 'to', 'for', 'of', 'and', 'or', 'a', 'an', 'the', 'is', 'it'];
            if (idx > 0 && smallWords.includes(lw)) {
                return lw;
            }
            return w.charAt(0).toUpperCase() + w.slice(1);
        })
        .join(' ');

    if (clean.length > 40) {
        clean = clean.substring(0, 38).trim() + '...';
    }

    return clean || 'Conversation Thread';
}

function renderChatSitemap(conversations = userConversations, filterQuery = '') {
    const list = document.getElementById('chatSitemapList');
    const badge = document.getElementById('chatHistoryCountBadge');
    if (!list) return;

    if (badge) {
        badge.textContent = userConversations.length;
    }

    const filtered = filterQuery 
        ? conversations.filter(c => {
            const matchTitle = c.title && c.title.toLowerCase().includes(filterQuery.toLowerCase());
            const matchTurns = c.turns && c.turns.some(t => 
                (t.question && t.question.toLowerCase().includes(filterQuery.toLowerCase())) ||
                (t.answer && t.answer.toLowerCase().includes(filterQuery.toLowerCase()))
            );
            return matchTitle || matchTurns;
        })
        : conversations;

    if (!filtered || filtered.length === 0) {
        if (filterQuery) {
            list.innerHTML = `
                <div class="chat-sitemap-empty">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                        <circle cx="11" cy="11" r="8"></circle>
                        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                    </svg>
                    <span>No conversations match "${escapeHtml(filterQuery)}"</span>
                </div>
            `;
        } else {
            list.innerHTML = `
                <div class="chat-sitemap-empty">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                    </svg>
                    <span>No conversations recorded yet. Ask a question to begin!</span>
                </div>
            `;
        }
        return;
    }

    list.innerHTML = '';

    filtered.forEach(conv => {
        const item = document.createElement('div');
        item.className = 'chat-sitemap-item' + (conv.id === activeConversationId ? ' active' : '');
        item.setAttribute('data-conv-id', conv.id);

        const timeStr = formatChatDateShort(conv.updatedAt || conv.createdAt);
        const msgCount = conv.turns ? conv.turns.length : 1;
        const msgBadge = `<span class="sitemap-meta-badge">💬 ${msgCount} ${msgCount === 1 ? 'msg' : 'msgs'}</span>`;
        const citationBadge = conv.totalCitations > 0 
            ? `<span class="sitemap-meta-badge">📄 ${conv.totalCitations} ${conv.totalCitations === 1 ? 'citation' : 'citations'}</span>`
            : '';

        const firstQuestion = conv.turns?.[0]?.question || conv.title;

        item.innerHTML = `
            <div class="sitemap-item-title" title="${escapeHtml(firstQuestion)}">${escapeHtml(conv.title)}</div>
            <button type="button" class="sitemap-item-delete-btn" title="Delete this conversation" data-conv-delete="${conv.id}">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                </svg>
            </button>
            <div class="sitemap-item-meta">
                <span class="sitemap-meta-time">${timeStr}</span>
                <div style="display: flex; align-items: center; gap: 0.3rem;">
                    ${msgBadge}
                    ${citationBadge}
                </div>
            </div>
        `;

        item.addEventListener('click', (e) => {
            if (e.target.closest('.sitemap-item-delete-btn')) return;
            openConversation(conv.id);
        });

        const delBtn = item.querySelector('.sitemap-item-delete-btn');
        if (delBtn) {
            delBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                deleteSingleConversation(conv.id);
            });
        }

        list.appendChild(item);
    });
}

/**
 * Opens and renders the WHOLE CONVERSATION in the main chat view when clicked.
 */
function openConversation(convId) {
    if (!convId) return;

    const stream = document.getElementById('chatMessagesStream');
    if (!stream) return;

    const conv = userConversations.find(c => String(c.id) === String(convId));
    if (!conv) {
        console.warn('Conversation not found for id:', convId);
        return;
    }

    activeConversationId = conv.id;

    // 1. Highlight active item in sitemap
    const list = document.getElementById('chatSitemapList');
    if (list) {
        list.querySelectorAll('.chat-sitemap-item').forEach(el => el.classList.remove('active'));
        const activeItem = list.querySelector(`.chat-sitemap-item[data-conv-id="${conv.id}"]`);
        if (activeItem) activeItem.classList.add('active');
    }

    // 2. Clear current view
    stream.innerHTML = '';

    // 3. Render Top Banner with conversation title, message count, and quick "New Chat" button
    const banner = document.createElement('div');
    banner.className = 'chat-conversation-active-banner';
    const turnCount = conv.turns ? conv.turns.length : 0;
    banner.innerHTML = `
        <div class="conversation-banner-info">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            </svg>
            <span><strong>${escapeHtml(conv.title)}</strong> • ${turnCount} ${turnCount === 1 ? 'exchange' : 'exchanges'} • ${formatChatDateShort(conv.updatedAt || conv.createdAt)}</span>
        </div>
        <button type="button" class="btn-chat-ghost btn-banner-new-chat" id="btnBannerNewChat" title="Start a fresh chat">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            <span>New Chat</span>
        </button>
    `;
    stream.appendChild(banner);

    const bannerNewChatBtn = banner.querySelector('#btnBannerNewChat');
    if (bannerNewChatBtn) {
        bannerNewChatBtn.addEventListener('click', () => {
            resetChatToNewState();
        });
    }

    // 4. Render the WHOLE CONVERSATION: all turns chronologically!
    if (conv.turns && conv.turns.length > 0) {
        conv.turns.forEach(turn => {
            // Append User Question
            appendUserMessage(turn.question, turn.questionTime, false, turn.id);

            // Append Assistant Answer with Citations
            if (turn.answer) {
                appendAssistantMessage({
                    answer: turn.answer,
                    sources: turn.sources || [],
                    answerReference: turn.answerRef
                }, turn.answerTime || turn.questionTime, false, turn.id);
            }
        });
    }

    // 5. Scroll smoothly to bottom
    scrollChatToBottom(false);

    // 6. Focus input so user can continue asking follow-ups in this conversation
    const chatInput = document.getElementById('chatInputText');
    if (chatInput) {
        chatInput.focus();
    }
}

async function deleteSingleConversation(convId) {
    if (!convId) return;
    const conv = userConversations.find(c => c.id === convId);
    const title = conv?.title || 'this conversation';
    if (!confirm(`Are you sure you want to delete "${title}"?`)) {
        return;
    }

    const username = AppState.currentUser?.username || 'anonymous';
    try {
        await fetch(`/api/knowledge/ask/history?username=${encodeURIComponent(username)}&conversationId=${encodeURIComponent(convId)}`, {
            method: 'DELETE'
        });

        userConversations = userConversations.filter(c => c.id !== convId);
        userConversationTurns = userConversations.flatMap(c => c.turns);
        renderChatSitemap(userConversations);

        if (activeConversationId === convId) {
            resetChatToNewState();
        }
        showAlert(`Deleted conversation "${title}".`, 'success');
    } catch (err) {
        console.error('Failed to delete conversation:', err);
        showAlert('Failed to delete conversation.', 'danger');
    }
}

function scrollChatToBottom(smooth = true) {
    const scrollContainer = document.getElementById('chatMessagesScroll');
    if (scrollContainer) {
        if (smooth) {
            scrollContainer.scrollTo({ top: scrollContainer.scrollHeight, behavior: 'smooth' });
        } else {
            scrollContainer.scrollTop = scrollContainer.scrollHeight;
        }
    }
}

function resetChatToNewState() {
    activeConversationId = null;
    renderChatWelcomeState();
    const sitemapList = document.getElementById('chatSitemapList');
    if (sitemapList) {
        sitemapList.querySelectorAll('.chat-sitemap-item').forEach(el => el.classList.remove('active'));
    }
    const chatInput = document.getElementById('chatInputText');
    if (chatInput) {
        chatInput.value = '';
        chatInput.focus();
    }
}

function renderChatWelcomeState() {
    const stream = document.getElementById('chatMessagesStream');
    if (!stream) return;

    stream.innerHTML = `
        <div class="chat-welcome-card" id="chatWelcomeCard">
            <div class="chat-welcome-avatar">
                <img src="images/agivant-logo-gradient.png" alt="Agivant AI" class="welcome-logo-img">
            </div>
            <h3>Welcome to Agivant AI Assistant</h3>
            <p>Ask questions in plain English. The agent searches approved company documents within your clearance level, performs multi-evidence synthesis, and provides exact citations.</p>
            <div class="welcome-suggestions-grid">
                <button type="button" class="welcome-suggestion-btn" data-query="How many leaves did I have?">
                    <span class="suggestion-icon">🏖️</span>
                    <div>
                        <strong>My Leaves Quota</strong>
                        <span>View personal leave balance & policy</span>
                    </div>
                </button>
                <button type="button" class="welcome-suggestion-btn" data-query="What is the carry-over policy for annual leave?">
                    <span class="suggestion-icon">📋</span>
                    <div>
                        <strong>Annual Leave Carry-Over</strong>
                        <span>Inspect HR policy on unused leave days</span>
                    </div>
                </button>
                <button type="button" class="welcome-suggestion-btn" data-query="What is the allowance for remote work monitors?">
                    <span class="suggestion-icon">💻</span>
                    <div>
                        <strong>Remote Work Allowance</strong>
                        <span>Home office equipment & stipend details</span>
                    </div>
                </button>
                <button type="button" class="welcome-suggestion-btn" data-query="Tell me how many technical guides are there?">
                    <span class="suggestion-icon">📚</span>
                    <div>
                        <strong>Repository Guide Count</strong>
                        <span>Technical manuals and runbooks available</span>
                    </div>
                </button>
            </div>
        </div>
    `;

    stream.querySelectorAll('.welcome-suggestion-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const query = btn.getAttribute('data-query');
            if (query) sendChatMessage(query);
        });
    });
}

async function loadUserChatHistory(username) {
    if (!username) return;
    activeChatUser = username;

    updateUserSitemapBar(AppState.currentUser);

    const stream = document.getElementById('chatMessagesStream');
    if (!stream) return;

    userConversations = [];
    userConversationTurns = [];
    renderChatSitemap([]);

    try {
        const res = await fetch(`/api/knowledge/ask/history?username=${encodeURIComponent(username)}`);
        if (res.ok) {
            const history = await res.json();
            if (Array.isArray(history) && history.length > 0) {
                const convMap = new Map();
                let currentConvId = null;
                let lastMsgTime = null;
                let syntheticConvIdx = 1;

                history.forEach(msg => {
                    let convId = msg.conversationId;
                    const msgTime = msg.createdAt ? new Date(msg.createdAt).getTime() : Date.now();

                    if (!convId || !convId.trim()) {
                        // 15-minute gap heuristic for legacy messages
                        if (currentConvId && lastMsgTime && (msgTime - lastMsgTime <= 15 * 60 * 1000)) {
                            convId = currentConvId;
                        } else {
                            convId = `conv_legacy_${msgTime}_${syntheticConvIdx++}`;
                            currentConvId = convId;
                        }
                    } else {
                        convId = convId.trim();
                        currentConvId = convId;
                    }
                    lastMsgTime = msgTime;

                    if (!convMap.has(convId)) {
                        convMap.set(convId, {
                            id: convId,
                            title: '',
                            createdAt: msg.createdAt,
                            updatedAt: msg.createdAt,
                            totalCitations: 0,
                            turns: []
                        });
                    }

                    const conv = convMap.get(convId);
                    conv.updatedAt = msg.createdAt;

                    if (msg.role === 'user') {
                        const turnId = 'turn-' + (msg.id || Date.now() + '-' + Math.random().toString(36).substring(2, 7));
                        conv.turns.push({
                            id: turnId,
                            question: msg.content,
                            questionTime: msg.createdAt,
                            answer: '',
                            answerTime: null,
                            sources: [],
                            sourcesCount: 0,
                            answerRef: null
                        });
                    } else if (msg.role === 'assistant') {
                        let sources = [];
                        if (msg.sourcesJson) {
                            try { sources = JSON.parse(msg.sourcesJson); } catch (e) {}
                        }
                        const lastTurn = conv.turns[conv.turns.length - 1];
                        if (lastTurn) {
                            lastTurn.answer = msg.content;
                            lastTurn.answerTime = msg.createdAt;
                            lastTurn.sources = Array.isArray(sources) ? sources : [];
                            lastTurn.sourcesCount = lastTurn.sources.length;
                            lastTurn.answerRef = msg.answerReference;
                            conv.totalCitations += lastTurn.sourcesCount;
                        }
                    }
                });

                userConversations = Array.from(convMap.values()).filter(c => c.turns.length > 0);
                userConversations.forEach(conv => {
                    const firstQ = conv.turns[0]?.question || '';
                    const secondQ = conv.turns[1]?.question || '';
                    conv.title = summarizeConversationTitle(firstQ, secondQ);
                });

                // Sort by latest activity first (newest conversation on top)
                userConversations.sort((a, b) => {
                    const timeA = new Date(a.updatedAt || a.createdAt || 0).getTime();
                    const timeB = new Date(b.updatedAt || b.createdAt || 0).getTime();
                    return timeB - timeA;
                });

                // Populate userConversationTurns for backwards compatibility
                userConversationTurns = userConversations.flatMap(c => c.turns);

                renderChatSitemap(userConversations);

                // Always present a clean, fresh New Chat welcome card
                resetChatToNewState();
                return;
            }
        }
    } catch (err) {
        console.warn('Failed to fetch chat history from server:', err);
    }

    // Default to clean welcome state if no messages
    resetChatToNewState();
    renderChatSitemap([]);
}

function appendUserMessage(text, timestamp, shouldScroll = true, turnId = null) {
    const stream = document.getElementById('chatMessagesStream');
    if (!stream) return;

    const welcome = document.getElementById('chatWelcomeCard');
    if (welcome) welcome.remove();

    const timeStr = formatChatTime(timestamp || new Date());
    const initials = (AppState.currentUser?.fullName || 'User')
        .split(' ')
        .map(n => n.charAt(0))
        .join('')
        .substring(0, 2)
        .toUpperCase();

    const msgRow = document.createElement('div');
    msgRow.className = 'chat-message-row user-row';
    if (turnId) {
        msgRow.id = `chatTurnRow-${turnId}`;
    }
    msgRow.innerHTML = `
        <div class="chat-message-body">
            <div class="chat-bubble user-bubble">${escapeHtml(text)}</div>
            <div class="chat-message-meta">
                <span>${timeStr}</span>
            </div>
        </div>
        <div class="chat-msg-avatar user-avatar-badge" title="${escapeHtml(AppState.currentUser?.fullName || 'User')}">
            <span>${initials}</span>
        </div>
    `;

    stream.appendChild(msgRow);
    if (shouldScroll) scrollChatToBottom(true);
}

function appendAssistantMessage(data, timestamp, shouldScroll = true, turnId = null) {
    const stream = document.getElementById('chatMessagesStream');
    if (!stream) return;

    const welcome = document.getElementById('chatWelcomeCard');
    if (welcome) welcome.remove();

    const timeStr = formatChatTime(timestamp || new Date());
    const answerRef = data.answerReference || 'ANS-' + Date.now();
    const sources = Array.isArray(data.sources) ? data.sources : [];

    // Format synthesized answer HTML
    const formattedAnswerHtml = formatRichAiAnswer(data.answer);

    // Citations Accordion
    let sourcesAccordionHtml = '';
    if (sources.length > 0) {
        sourcesAccordionHtml = `
            <div class="bubble-citations-accordion">
                <button type="button" class="bubble-citations-toggle" onclick="this.parentElement.classList.toggle('open')">
                    <div style="display: flex; align-items: center; gap: 0.4rem;">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                        </svg>
                        <span>Verified Supporting Sources (${sources.length})</span>
                    </div>
                    <svg class="chevron-svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <polyline points="6 9 12 15 18 9"></polyline>
                    </svg>
                </button>
                <div class="bubble-citations-body">
                    ${sources.map(src => {
                        const cleanExcerpt = sanitizePassageText(src.excerpt);
                        const sectionRef = formatSectionReference(src.sectionReference);
                        return `
                            <div class="bubble-citation-card">
                                <div class="citation-head">
                                    <span class="citation-doc-name">📄 ${escapeHtml(src.documentTitle || 'Document #' + src.documentId)}</span>
                                    <span class="badge badge-employee" style="font-size: 0.72rem;">v${src.documentVersion || 1}</span>
                                    <span class="citation-sec">📍 ${escapeHtml(sectionRef)}</span>
                                </div>
                                <p class="citation-excerpt">"${escapeHtml(cleanExcerpt)}"</p>
                                <div style="display: flex; justify-content: flex-end;">
                                    <button type="button" class="btn-link-sm" onclick="openSourcePreviewByDocAndChunk(${src.documentId}, ${src.chunkIndex})">
                                        🔍 Inspect Full Passage
                                    </button>
                                </div>
                            </div>
                        `;
                    }).join('')}
                </div>
            </div>
        `;
    }

    const msgRow = document.createElement('div');
    msgRow.className = 'chat-message-row assistant-row';
    if (turnId) {
        msgRow.setAttribute('data-turn-ref', turnId);
    }
    msgRow.innerHTML = `
        <div class="chat-msg-avatar assistant-avatar" title="Agivant AI Assistant">
            <img src="images/agivant-logo-gradient.png" alt="Agivant AI" class="avatar-mini">
        </div>
        <div class="chat-message-body">
            <div class="chat-bubble assistant-bubble">
                <div class="bubble-header">
                    <span class="bubble-assistant-name">Agivant AI Assistant</span>
                    <span class="bubble-ref-tag">${escapeHtml(answerRef)}</span>
                </div>
                <div class="bubble-content-text">
                    ${formattedAnswerHtml}
                </div>
                ${sourcesAccordionHtml}
                <div class="bubble-actions-row">
                    <div style="display: flex; align-items: center; gap: 0.45rem; flex-wrap: wrap;">
                        <button type="button" class="bubble-action-btn btn-copy-chat" title="Copy answer">
                            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                            </svg>
                            <span>Copy</span>
                        </button>
                        <button type="button" class="bubble-action-btn btn-toggle-feedback-comment" title="Write feedback comment">
                            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                            </svg>
                            <span>Write Feedback</span>
                        </button>
                    </div>
                    <div class="bubble-rating-widget" data-ref="${escapeHtml(answerRef)}">
                        <span class="rating-label">Rate:</span>
                        <button type="button" class="bubble-star" data-star="1" title="1 Star">★</button>
                        <button type="button" class="bubble-star" data-star="2" title="2 Stars">★</button>
                        <button type="button" class="bubble-star" data-star="3" title="3 Stars">★</button>
                        <button type="button" class="bubble-star" data-star="4" title="4 Stars">★</button>
                        <button type="button" class="bubble-star" data-star="5" title="5 Stars">★</button>
                        <span class="bubble-rating-ack"></span>
                    </div>
                </div>

                <!-- Expandable Feedback & Comment Card -->
                <div class="bubble-feedback-comment-card" style="display: none;">
                    <div class="feedback-comment-head">
                        <span class="feedback-comment-title">Provide Answer Feedback</span>
                        <span class="feedback-comment-rating-tag">Choose Rating</span>
                    </div>
                    <div class="feedback-comment-body">
                        <textarea class="feedback-comment-textarea" placeholder="Write your feedback comment (e.g. what worked well, accuracy, or what could be improved)..." rows="2" maxlength="1000"></textarea>
                        <div class="feedback-comment-actions">
                            <button type="button" class="btn-submit-feedback" title="Submit feedback and comment">Submit Feedback</button>
                            <button type="button" class="btn-cancel-feedback" title="Cancel">Cancel</button>
                        </div>
                    </div>
                </div>
                <div class="bubble-feedback-confirmed" style="display: none;"></div>
            </div>
            <div class="chat-message-meta">
                <span>${timeStr}</span>
            </div>
        </div>
    `;

    // Attach Copy Handler
    const copyBtn = msgRow.querySelector('.btn-copy-chat');
    if (copyBtn) {
        copyBtn.addEventListener('click', async () => {
            try {
                await navigator.clipboard.writeText(data.answer);
                copyBtn.innerHTML = `
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"></polyline></svg>
                    <span>Copied!</span>
                `;
                setTimeout(() => {
                    copyBtn.innerHTML = `
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                        </svg>
                        <span>Copy</span>
                    `;
                }, 2000);
            } catch (err) {
                showAlert('Answer copied to clipboard!', 'success');
            }
        });
    }

    // Attach Rating & Feedback Comment Handler
    let selectedRating = 0;
    const ratingWidget = msgRow.querySelector('.bubble-rating-widget');
    const feedbackDrawer = msgRow.querySelector('.bubble-feedback-comment-card');
    const toggleCommentBtn = msgRow.querySelector('.btn-toggle-feedback-comment');
    const ratingTag = msgRow.querySelector('.feedback-comment-rating-tag');
    const textarea = msgRow.querySelector('.feedback-comment-textarea');
    const submitBtn = msgRow.querySelector('.btn-submit-feedback');
    const cancelBtn = msgRow.querySelector('.btn-cancel-feedback');
    const confirmedBox = msgRow.querySelector('.bubble-feedback-confirmed');

    function updateStarsUi(rating) {
        if (!ratingWidget) return;
        const stars = ratingWidget.querySelectorAll('.bubble-star');
        stars.forEach(s => {
            const sVal = parseInt(s.getAttribute('data-star'), 10);
            s.classList.toggle('active', sVal <= rating);
        });
        if (ratingTag) {
            ratingTag.textContent = rating > 0 ? `${rating} of 5 Stars (${'★'.repeat(rating)})` : 'Choose Star Rating';
        }
    }

    if (ratingWidget) {
        const stars = ratingWidget.querySelectorAll('.bubble-star');
        const ack = ratingWidget.querySelector('.bubble-rating-ack');

        stars.forEach(star => {
            star.addEventListener('click', () => {
                const val = parseInt(star.getAttribute('data-star'), 10);
                selectedRating = val;
                updateStarsUi(selectedRating);

                // Open the feedback drawer so user can write a comment
                if (feedbackDrawer) {
                    feedbackDrawer.style.display = 'flex';
                    if (textarea) textarea.focus();
                }
                if (ack) ack.textContent = '⭐ ' + val + '/5 Selected';
            });
        });
    }

    if (toggleCommentBtn) {
        toggleCommentBtn.addEventListener('click', () => {
            if (!feedbackDrawer) return;
            if (feedbackDrawer.style.display === 'none' || !feedbackDrawer.style.display) {
                feedbackDrawer.style.display = 'flex';
                if (selectedRating === 0) {
                    selectedRating = 5; // default to 5 if opened directly
                    updateStarsUi(selectedRating);
                }
                if (textarea) textarea.focus();
            } else {
                feedbackDrawer.style.display = 'none';
            }
        });
    }

    if (cancelBtn) {
        cancelBtn.addEventListener('click', () => {
            if (feedbackDrawer) feedbackDrawer.style.display = 'none';
        });
    }

    if (submitBtn) {
        submitBtn.addEventListener('click', async () => {
            const ratingVal = selectedRating > 0 ? selectedRating : 5;
            const commentVal = textarea ? textarea.value.trim() : '';
            const ack = ratingWidget ? ratingWidget.querySelector('.bubble-rating-ack') : null;

            if (submitBtn) submitBtn.disabled = true;
            if (ack) ack.textContent = 'Saving...';

            try {
                const docId = (sources.length > 0) ? sources[0].documentId : null;
                const res = await fetch('/api/knowledge/feedback', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        documentId: docId,
                        question: data.question || '',
                        answerReference: answerRef,
                        rating: ratingVal,
                        comment: commentVal,
                        answerText: data.answer || ''
                    })
                });

                if (res.ok) {
                    if (ack) ack.textContent = '✓ Feedback Submitted';
                    if (feedbackDrawer) feedbackDrawer.style.display = 'none';
                    if (confirmedBox) {
                        confirmedBox.className = 'bubble-submitted-feedback';
                        confirmedBox.style.display = 'flex';
                        if (commentVal) {
                            confirmedBox.innerHTML = `✓ Feedback submitted (${ratingVal}/5 stars): &ldquo;<em>${escapeHtml(commentVal)}</em>&rdquo;`;
                        } else {
                            confirmedBox.innerHTML = `✓ Feedback submitted (${ratingVal}/5 stars).`;
                        }
                    }
                    if (toggleCommentBtn) toggleCommentBtn.style.display = 'none';
                } else {
                    if (ack) ack.textContent = '✓ Recorded';
                    if (feedbackDrawer) feedbackDrawer.style.display = 'none';
                }
            } catch (err) {
                console.error('Failed to submit feedback:', err);
                if (ack) ack.textContent = '✓ Recorded';
                if (feedbackDrawer) feedbackDrawer.style.display = 'none';
            } finally {
                if (submitBtn) submitBtn.disabled = false;
            }
        });
    }

    stream.appendChild(msgRow);
    if (shouldScroll) scrollChatToBottom(true);
}

async function sendChatMessage(questionText) {
    if (!questionText || !questionText.trim()) return;
    if (isChatSubmitting) return;

    const query = questionText.trim();
    const chatInput = document.getElementById('chatInputText');
    const sendBtn = document.getElementById('btnChatSend');
    const typingIndicator = document.getElementById('chatTypingIndicator');

    if (chatInput) {
        chatInput.value = '';
        chatInput.style.height = 'auto';
    }

    isChatSubmitting = true;
    if (sendBtn) sendBtn.disabled = true;

    const turnId = 'turn-' + Date.now();
    const nowIso = new Date().toISOString();

    // 1. Append user message
    appendUserMessage(query, nowIso, true, turnId);

    // 2. Show typing indicator & scroll
    const stream = document.getElementById('chatMessagesStream');
    let typingInterval = null;
    if (typingIndicator && stream) {
        stream.appendChild(typingIndicator);
        typingIndicator.style.display = 'flex';
        const typingText = document.getElementById('chatTypingText') || typingIndicator.querySelector('.typing-text');
        if (typingText) {
            typingText.textContent = 'Agivant AI is thinking...';
        }
        let step = 0;
        typingInterval = setInterval(() => {
            step++;
            if (!typingText) return;
            if (step === 1) {
                typingText.textContent = 'Searching company documents...';
            } else if (step === 2) {
                typingText.textContent = 'Synthesizing verified answer...';
            } else if (step >= 3) {
                typingText.textContent = 'Preparing response...';
            }
        }, 1800);
        scrollChatToBottom(true);
    }

    const username = AppState.currentUser?.username || 'anonymous';

    try {
        const payload = { question: query };
        if (activeConversationId) {
            payload.conversationId = activeConversationId;
        }

        const res = await fetch(`/api/knowledge/ask?username=${encodeURIComponent(username)}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (typingInterval) clearInterval(typingInterval);
        if (typingIndicator) typingIndicator.style.display = 'none';

        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            const errMsg = err.message || "We couldn't retrieve an answer right now. Please check if your question relates to internal company documents and try again.";
            appendAssistantMessage({
                answer: errMsg,
                sources: [],
                answerReference: 'ERR-RAG'
            }, new Date().toISOString(), true, turnId);
            return;
        }

        const data = await res.json();
        data.question = query;
        appendAssistantMessage(data, new Date().toISOString(), true, turnId);

        const convId = data.conversationId || activeConversationId;
        activeConversationId = convId;

        const sourcesCount = Array.isArray(data.sources) ? data.sources.length : 0;
        const newTurn = {
            id: turnId,
            question: query,
            questionTime: nowIso,
            answer: data.answer,
            answerTime: new Date().toISOString(),
            sources: data.sources || [],
            sourcesCount: sourcesCount,
            answerRef: data.answerReference
        };

        let existingConv = userConversations.find(c => c.id === convId);
        if (existingConv) {
            existingConv.turns.push(newTurn);
            existingConv.updatedAt = new Date().toISOString();
            existingConv.totalCitations = (existingConv.totalCitations || 0) + sourcesCount;
            // Move conversation to top
            userConversations = [existingConv, ...userConversations.filter(c => c.id !== convId)];
        } else {
            const newConv = {
                id: convId,
                title: summarizeConversationTitle(query),
                createdAt: nowIso,
                updatedAt: new Date().toISOString(),
                totalCitations: sourcesCount,
                turns: [newTurn]
            };
            userConversations.unshift(newConv);
        }

        userConversationTurns = userConversations.flatMap(c => c.turns);
        renderChatSitemap(userConversations);

        // Highlight active item in sitemap
        const sitemapList = document.getElementById('chatSitemapList');
        if (sitemapList) {
            sitemapList.querySelectorAll('.chat-sitemap-item').forEach(el => el.classList.remove('active'));
            const activeItem = sitemapList.querySelector(`.chat-sitemap-item[data-conv-id="${convId}"]`);
            if (activeItem) activeItem.classList.add('active');
        }

    } catch (err) {
        console.error('Chat error:', err);
        if (typingInterval) clearInterval(typingInterval);
        if (typingIndicator) typingIndicator.style.display = 'none';
        appendAssistantMessage({
            answer: "Network error communicating with AI agent. Please check your network and try again.",
            sources: [],
            answerReference: 'NET-ERR'
        }, new Date().toISOString(), true, turnId);
    } finally {
        if (typingInterval) clearInterval(typingInterval);
        if (typingIndicator) typingIndicator.style.display = 'none';
        isChatSubmitting = false;
        if (sendBtn) sendBtn.disabled = false;
        if (chatInput) chatInput.focus();
    }
}

function initAiAgent() {
    const chatForm = document.getElementById('chatMessageForm');
    const chatInput = document.getElementById('chatInputText');
    const chips = document.querySelectorAll('.chat-chip');
    const btnNewChat = document.getElementById('btnNewChat');
    const btnClearChatHistory = document.getElementById('btnClearChatHistory');
    const btnToggleSitemap = document.getElementById('btnToggleChatSitemap');
    const sitemapPanel = document.getElementById('chatSitemapPanel');
    const sitemapSearchInput = document.getElementById('chatHistorySearchInput');

    // Auto-resizing textarea & Enter key to submit
    if (chatInput) {
        chatInput.addEventListener('input', () => {
            chatInput.style.height = 'auto';
            chatInput.style.height = Math.min(chatInput.scrollHeight, 120) + 'px';
        });

        chatInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                if (chatForm) {
                    const submitEvent = new Event('submit', { cancelable: true });
                    chatForm.dispatchEvent(submitEvent);
                }
            }
        });
    }

    // Chat form submit
    if (chatForm) {
        chatForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const text = chatInput ? chatInput.value : '';
            sendChatMessage(text);
        });
    }

    // Quick Chips click
    chips.forEach(chip => {
        chip.addEventListener('click', () => {
            const query = chip.getAttribute('data-query');
            if (query) sendChatMessage(query);
        });
    });

    // New Chat button
    if (btnNewChat) {
        btnNewChat.addEventListener('click', () => {
            resetChatToNewState();
        });
    }

    // Toggle Sitemap button
    if (btnToggleSitemap && sitemapPanel) {
        btnToggleSitemap.addEventListener('click', () => {
            if (sitemapPanel.style.display === 'none') {
                sitemapPanel.style.display = 'flex';
            } else {
                sitemapPanel.style.display = 'none';
            }
        });
    }

    // Live Search in Sitemap
    if (sitemapSearchInput) {
        sitemapSearchInput.addEventListener('input', (e) => {
            const q = e.target.value.trim();
            renderChatSitemap(userConversations, q);
        });
    }

    // Clear History button
    if (btnClearChatHistory) {
        btnClearChatHistory.addEventListener('click', async () => {
            if (!confirm('Are you sure you want to clear your entire conversation history? This cannot be undone.')) {
                return;
            }

            const username = AppState.currentUser?.username || 'anonymous';
            try {
                await fetch(`/api/knowledge/ask/history?username=${encodeURIComponent(username)}`, {
                    method: 'DELETE'
                });
                userConversations = [];
                userConversationTurns = [];
                activeConversationId = null;
                resetChatToNewState();
                renderChatSitemap([]);
                const badge = document.getElementById('chatHistoryCountBadge');
                if (badge) badge.textContent = '0';
                showAlert('All conversation history cleared.', 'success');
            } catch (err) {
                console.error('Failed to clear chat history:', err);
                showAlert('Failed to clear conversation history.', 'danger');
            }
        });
    }

    // If user is already set, load their history
    if (AppState.currentUser?.username) {
        loadUserChatHistory(AppState.currentUser.username);
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

    const uploadCategorySelect = document.getElementById('uploadCategory');
    const customCatGroup = document.getElementById('uploadCustomCategoryGroup');
    const customCatInput = document.getElementById('uploadCustomCategory');

    if (uploadCategorySelect && customCatGroup) {
        uploadCategorySelect.addEventListener('change', () => {
            if (uploadCategorySelect.value === 'OTHER') {
                customCatGroup.style.display = 'flex';
                if (customCatInput) {
                    customCatInput.setAttribute('required', 'required');
                    customCatInput.focus();
                }
            } else {
                customCatGroup.style.display = 'none';
                if (customCatInput) {
                    customCatInput.removeAttribute('required');
                    customCatInput.value = '';
                }
            }
        });
    }

    if (uploadForm) {
        uploadForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const title = document.getElementById('uploadTitle').value.trim();
            const description = document.getElementById('uploadDescription').value.trim();
            const category = document.getElementById('uploadCategory').value;
            const customCategory = document.getElementById('uploadCustomCategory')?.value.trim() || '';
            const accessLevel = document.getElementById('uploadAccessLevel').value;
            const fileInput = document.getElementById('uploadFile');

            if (category === 'OTHER' && !customCategory) {
                showAlert('Please enter a custom category name.', 'danger');
                document.getElementById('uploadCustomCategory')?.focus();
                return;
            }

            if (!fileInput.files || fileInput.files.length === 0) {
                showAlert('Please select a file to upload.', 'danger');
                return;
            }

            const formData = new FormData();
            formData.append('title', title);
            formData.append('description', description);
            formData.append('category', category);
            if (category === 'OTHER' && customCategory) {
                formData.append('customCategory', customCategory);
            }
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
                    if (customCatGroup) customCatGroup.style.display = 'none';
                    if (customCatInput) {
                        customCatInput.removeAttribute('required');
                        customCatInput.value = '';
                    }
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

    const resetBtn = document.getElementById('uploadResetBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            resetDocumentUploadForm();
            showAlert('Document upload form cleared.', 'info');
        });
    }

    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            resetDocumentUploadForm();
            loadDocumentsTable();
        });
    }
    if (catFilter) catFilter.addEventListener('change', loadDocumentsTable);
    if (statusFilter) statusFilter.addEventListener('change', loadDocumentsTable);
}

function resetDocumentUploadForm() {
    const uploadForm = document.getElementById('documentUploadForm');
    if (uploadForm) {
        uploadForm.reset();
    }
    const customCatGroup = document.getElementById('uploadCustomCategoryGroup');
    const customCatInput = document.getElementById('uploadCustomCategory');
    if (customCatGroup) {
        customCatGroup.style.display = 'none';
    }
    if (customCatInput) {
        customCatInput.removeAttribute('required');
        customCatInput.value = '';
    }
    const fileInput = document.getElementById('uploadFile');
    if (fileInput) {
        fileInput.value = '';
    }
}

async function loadDocumentsTable() {
    const catFilter = document.getElementById('docListCategoryFilter')?.value || '';
    const statusFilter = document.getElementById('docListStatusFilter')?.value || 'ACTIVE';
    const tbody = document.getElementById('documentsTableTbody');

    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="10" class="text-center">Loading documents...</td></tr>';

    let url = `/api/knowledge/documents?status=${statusFilter}`;
    if (catFilter) url += `&category=${catFilter}`;

    try {
        const res = await fetch(url);
        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="10" class="text-center">Failed to load documents.</td></tr>';
            return;
        }

        const documents = await res.json();
        AppState.accessibleDocuments = documents;

        const countBadge = document.getElementById('docTableCountBadge');
        if (countBadge) {
            countBadge.textContent = `${documents.length} ${statusFilter === 'ARCHIVED' ? 'Archived' : 'Available'}`;
        }
        const headerTitle = document.getElementById('docTableHeaderTitle');
        if (headerTitle) {
            headerTitle.textContent = statusFilter === 'ARCHIVED' ? 'Archived Company Documents' : 'Available Company Documents';
        }

        if (documents.length === 0) {
            tbody.innerHTML = '<tr><td colspan="10" class="text-center">No documents found matching the filter criteria.</td></tr>';
            return;
        }

        tbody.innerHTML = documents.map(doc => {
            const accessBadgeClass = `badge-${doc.accessLevel.toLowerCase()}`;
            const isStatusActive = doc.status === 'ACTIVE';
            const statusBadgeClass = isStatusActive ? 'badge-active' : 'badge-archived';
            const indexingStatus = doc.indexingStatus || 'INDEXED';
            let indexingBadgeClass = 'badge-success';
            if (indexingStatus === 'FAILED') indexingBadgeClass = 'badge-admin';
            else if (indexingStatus === 'PENDING') indexingBadgeClass = 'badge-mode';

            const dateStr = doc.updatedAt ? new Date(doc.updatedAt).toLocaleDateString() : '-';

            return `
                <tr>
                    <td><strong>#${doc.id}</strong></td>
                    <td>${escapeHtml(doc.title)}</td>
                    <td>${formatCategoryLabel(doc.category, doc.customCategory)}</td>
                    <td><span class="badge ${accessBadgeClass}">${escapeHtml(formatAccessLevelLabel(doc.accessLevel))}</span></td>
                    <td>v${doc.version}</td>
                    <td><span class="badge ${statusBadgeClass}">${escapeHtml(formatStatusLabel(doc.status))}</span></td>
                    <td><span class="badge ${indexingBadgeClass}">${escapeHtml(indexingStatus)}</span></td>
                    <td>${doc.chunkCount}</td>
                    <td>${dateStr}</td>
                    <td>
                        <div style="display: flex; gap: 0.35rem; flex-wrap: wrap;">
                            <button class="btn btn-secondary btn-sm" onclick="viewDocumentDetails(${doc.id})">Details</button>
                            <button class="btn btn-secondary btn-sm" onclick="openVersionHistoryModal(${doc.id}, '${escapeHtml(doc.title).replace(/'/g, "\\'")}')">History</button>
                            ${(AppState.currentUser?.role === 'ROLE_ADMIN' && isStatusActive) ? `
                                <button class="btn btn-secondary btn-sm" onclick="reindexDocument(${doc.id})">Re-index</button>
                                <button class="btn btn-secondary btn-sm" style="color: var(--danger);" onclick="archiveDocument(${doc.id})">Archive</button>
                            ` : ''}
                            ${(AppState.currentUser?.role === 'ROLE_ADMIN' && !isStatusActive) ? `
                                <button class="btn btn-primary btn-sm" onclick="restoreDocument(${doc.id})">Restore</button>
                            ` : ''}
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        console.error('Load documents error:', err);
        tbody.innerHTML = '<tr><td colspan="10" class="text-center">Error loading documents.</td></tr>';
    }
}

async function viewDocumentDetails(id) {
    await openFullDocumentReader(id);
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
            loadDashboardData();
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
        const res = await fetch(`/api/knowledge/documents/${id}/archive`, { method: 'POST' });
        if (res.ok) {
            showAlert(`Document #${id} has been safely archived.`, 'success');
            loadDocumentsTable();
            loadDashboardData();
        } else {
            const err = await res.json();
            showAlert(err.message || 'Failed to archive document.', 'danger');
        }
    } catch (err) {
        console.error('Archive error:', err);
    }
}

async function restoreDocument(id) {
    if (!confirm(`Are you sure you want to restore document #${id} back to Active status? It will immediately become available in search and Q&A.`)) {
        return;
    }
    try {
        const res = await fetch(`/api/knowledge/documents/${id}/restore`, { method: 'POST' });
        if (res.ok) {
            showAlert(`Document #${id} successfully restored to Active status!`, 'success');
            loadDocumentsTable();
            loadDashboardData();
        } else {
            const err = await res.json();
            showAlert(err.message || 'Failed to restore document.', 'danger');
        }
    } catch (err) {
        console.error('Restore error:', err);
    }
}

async function openVersionHistoryModal(documentId, documentTitle) {
    const modal = document.getElementById('versionHistoryModal');
    const titleEl = document.getElementById('versionModalDocTitle');
    const tbody = document.getElementById('versionHistoryTbody');

    if (!modal) return;
    if (titleEl) titleEl.textContent = `Version History: ${documentTitle || '#' + documentId}`;
    if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center">Loading version snapshots...</td></tr>';

    modal.style.display = 'flex';

    try {
        const res = await fetch(`/api/knowledge/documents/${documentId}/versions`);
        if (!res.ok) {
            if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Unable to retrieve versions.</td></tr>';
            return;
        }

        const versions = await res.json();
        if (!tbody) return;

        if (!Array.isArray(versions) || versions.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No historical versions recorded for this document yet.</td></tr>';
            return;
        }

        tbody.innerHTML = versions.map(v => {
            const dateStr = v.createdAt ? new Date(v.createdAt).toLocaleString() : '-';
            return `
                <tr>
                    <td><strong>v${v.versionNumber}</strong></td>
                    <td><code>${escapeHtml(v.originalFileName || '-')}</code></td>
                    <td><span class="badge badge-employee">${escapeHtml(v.accessLevel)}</span></td>
                    <td>${v.chunkCount}</td>
                    <td style="font-size: 0.85rem;">${escapeHtml(v.changeSummary || 'Version snapshot')}</td>
                    <td>${escapeHtml(formatUserWithRole(v.createdBy))}</td>
                    <td style="font-size: 0.82rem; color: var(--text-secondary);">${dateStr}</td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        console.error('Load version history error:', err);
        if (tbody) tbody.innerHTML = '<tr><td colspan="7" class="text-center">Error loading versions.</td></tr>';
    }
}

// ===================================================================
// SCREEN E: SOURCE PREVIEW & FULL DOCUMENT READER MODALS
// ===================================================================
function initModal() {
    // 1. Source Preview Chunk Modal
    const modal = document.getElementById('sourcePreviewModal');
    const closeBtn = document.getElementById('modalCloseBtn');
    const dismissBtn = document.getElementById('modalDismissBtn');

    const closeModal = () => { if (modal) modal.style.display = 'none'; };
    if (closeBtn) closeBtn.onclick = closeModal;
    if (dismissBtn) dismissBtn.onclick = closeModal;
    if (modal) modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });

    // 2. Full Document Reader Modal
    const fullDocModal = document.getElementById('fullDocumentModal');
    const fullDocCloseBtn = document.getElementById('fullDocCloseBtn');
    const fullDocDismissBtn = document.getElementById('fullDocDismissBtn');

    const closeFullDoc = () => { if (fullDocModal) fullDocModal.style.display = 'none'; };
    if (fullDocCloseBtn) fullDocCloseBtn.onclick = closeFullDoc;
    if (fullDocDismissBtn) fullDocDismissBtn.onclick = closeFullDoc;
    if (fullDocModal) fullDocModal.addEventListener('click', (e) => { if (e.target === fullDocModal) closeFullDoc(); });

    // 3. Document Version History Modal
    const verModal = document.getElementById('versionHistoryModal');
    const verCloseBtn = document.getElementById('versionModalCloseBtn');
    const verDismissBtn = document.getElementById('versionModalDismissBtn');

    const closeVerModal = () => { if (verModal) verModal.style.display = 'none'; };
    if (verCloseBtn) verCloseBtn.onclick = closeVerModal;
    if (verDismissBtn) verDismissBtn.onclick = closeVerModal;
    if (verModal) verModal.addEventListener('click', (e) => { if (e.target === verModal) closeVerModal(); });

    // 4. Feedback Details Inspection Modal
    const fbModal = document.getElementById('feedbackDetailsModal');
    const fbCloseBtn = document.getElementById('fbModalCloseBtn');
    const fbDismissBtn = document.getElementById('fbModalDismissBtn');

    const closeFbModal = () => { if (fbModal) fbModal.style.display = 'none'; };
    if (fbCloseBtn) fbCloseBtn.onclick = closeFbModal;
    if (fbDismissBtn) fbDismissBtn.onclick = closeFbModal;
    if (fbModal) fbModal.addEventListener('click', (e) => { if (e.target === fbModal) closeFbModal(); });

    // 5. Employee Details Inspection Modal
    const empModal = document.getElementById('employeeDetailsModal');
    const empCloseBtn = document.getElementById('btnEmpModalClose');
    const empDismissBtn = document.getElementById('btnEmpModalDismiss');
    const empBackdrop = document.getElementById('empModalBackdrop');

    const closeEmpModal = () => { if (empModal) empModal.style.display = 'none'; };
    if (empCloseBtn) empCloseBtn.onclick = closeEmpModal;
    if (empDismissBtn) empDismissBtn.onclick = closeEmpModal;
    if (empBackdrop) empBackdrop.onclick = closeEmpModal;
    if (empModal) empModal.addEventListener('click', (e) => { if (e.target === empModal) closeEmpModal(); });
}


async function openFullDocumentReader(documentId) {
    const modal = document.getElementById('fullDocumentModal');
    const titleEl = document.getElementById('fullDocTitle');
    const typeBadge = document.getElementById('fullDocTypeBadge');
    const catBadge = document.getElementById('fullDocCategoryBadge');
    const statusBadge = document.getElementById('fullDocStatusBadge');
    const versionBadge = document.getElementById('fullDocVersionBadge');
    const metaEl = document.getElementById('fullDocMeta');
    const bodyEl = document.getElementById('fullDocBody');
    const openTabBtn = document.getElementById('fullDocOpenTabBtn');
    const clearanceTag = document.getElementById('fullDocClearanceTag');

    if (!modal) return;

    // Reset and show modal in loading state
    titleEl.textContent = 'Loading document...';
    typeBadge.style.display = 'none';
    catBadge.style.display = 'none';
    if (statusBadge) statusBadge.style.display = 'none';
    if (versionBadge) versionBadge.style.display = 'none';
    metaEl.textContent = 'Checking access permissions...';
    clearanceTag.textContent = 'Verifying Clearance...';
    openTabBtn.style.display = 'none';
    bodyEl.innerHTML = `
        <div style="padding: 3rem 1rem; text-align: center; color: var(--text-muted);">
            <div class="spinner" style="margin: 0 auto 1rem auto;"></div>
            <p style="font-size: 0.95rem;">Retrieving complete document...</p>
        </div>
    `;
    modal.style.display = 'flex';

    try {
        // 1. Fetch document details to get metadata and verify access clearance
        const detailRes = await fetch(`/api/knowledge/documents/${documentId}`);
        if (!detailRes.ok) {
            modal.style.display = 'none';
            showAlert("You don't have access to this document.", 'danger');
            return;
        }

        const data = await detailRes.json();
        const doc = data.document;
        const chunks = data.chunks || [];

        titleEl.textContent = doc.title;
        catBadge.textContent = formatCategoryLabel(doc.category, doc.customCategory);
        catBadge.style.display = 'inline-block';

        if (statusBadge) {
            statusBadge.textContent = formatStatusLabel(doc.status);
            statusBadge.className = `badge ${doc.status === 'ACTIVE' ? 'badge-active' : 'badge-archived'}`;
            statusBadge.style.display = 'inline-block';
        }

        if (versionBadge) {
            versionBadge.textContent = `v${doc.version}`;
            versionBadge.style.display = 'inline-block';
        }

        const isPdf = (doc.contentType && doc.contentType.toLowerCase().includes('pdf')) ||
                      (doc.originalFileName && doc.originalFileName.toLowerCase().endsWith('.pdf'));

        const fileTypeLabel = isPdf ? 'PDF' : 'TXT';
        typeBadge.textContent = fileTypeLabel;
        typeBadge.className = isPdf ? 'badge badge-admin' : 'badge badge-mode';
        typeBadge.style.display = 'inline-block';

        // Compact metadata area with friendly labels
        const friendlyOwner = formatOwnerName(doc.owner);
        const friendlyAccess = formatAccessLevelLabel(doc.accessLevel);
        metaEl.textContent = `Author: ${escapeHtml(friendlyOwner)} • Access: ${escapeHtml(friendlyAccess)} • Version ${doc.version}`;
        clearanceTag.textContent = `✓ Access: ${escapeHtml(friendlyAccess)} Verified`;

        const fileUrl = `/api/knowledge/documents/${doc.id}/file`;
        openTabBtn.href = fileUrl;
        openTabBtn.style.display = 'inline-block';

        if (isPdf) {
            try {
                const fileRes = await fetch(fileUrl);
                const contentType = (fileRes.headers.get('content-type') || '').toLowerCase();
                if (fileRes.ok && contentType.includes('application/pdf')) {
                    // Native PDF binary is present on disk: embed browser PDF viewer
                    bodyEl.innerHTML = `
                        <iframe src="${fileUrl}" 
                                style="width: 100%; height: 68vh; border: 1px solid var(--border-color); border-radius: var(--radius-sm); background: #525659;" 
                                title="PDF Document Viewer">
                            <p>Your browser does not support embedded PDF viewing. 
                               <a href="${fileUrl}" target="_blank">Click here to open the PDF</a>.
                            </p>
                        </iframe>
                    `;
                } else if (fileRes.ok) {
                    // Fallback: If original binary PDF was cleared from ephemeral disk (e.g. Render restart),
                    // render the complete database searchable text in the clean structured reading view
                    const textContent = await fileRes.text();
                    bodyEl.innerHTML = renderStructuredDocumentContent(textContent, chunks, doc);
                } else {
                    bodyEl.innerHTML = renderStructuredDocumentContent('', chunks, doc);
                }
            } catch (err) {
                console.error('Error fetching document file:', err);
                bodyEl.innerHTML = renderStructuredDocumentContent('', chunks, doc);
            }
        } else {
            // Fetch complete document text and render structured layout
            const fileRes = await fetch(fileUrl);
            let textContent = '';
            if (fileRes.ok) {
                textContent = await fileRes.text();
            }
            bodyEl.innerHTML = renderStructuredDocumentContent(textContent, chunks, doc);
        }

    } catch (err) {
        console.error('Error opening full document:', err);
        modal.style.display = 'none';
        showAlert("We couldn't retrieve the document right now. Please try again.", 'danger');
    }
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
            showAlert("You don't have access to this document.", 'danger');
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
            showAlert("You don't have access to this document.", 'danger');
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
// SCREEN F: FEEDBACK MANAGEMENT (ADMIN & HR)
// ===================================================================
function initFeedback() {
    const refreshBtn = document.getElementById('btnRefreshFeedback');
    const filterBtn = document.getElementById('btnFilterFeedback');
    const clearBtn = document.getElementById('btnClearFeedbackFilter');
    const ratingFilter = document.getElementById('fbRatingFilter');
    const searchInput = document.getElementById('fbSearchInput');

    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => loadFeedbackManagement());
    }
    if (filterBtn) {
        filterBtn.addEventListener('click', () => loadFeedbackManagement());
    }
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            if (ratingFilter) ratingFilter.value = '';
            if (searchInput) searchInput.value = '';
            loadFeedbackManagement();
        });
    }
    if (ratingFilter) {
        ratingFilter.addEventListener('change', () => loadFeedbackManagement());
    }
    if (searchInput) {
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                loadFeedbackManagement();
            }
        });
    }
}

async function loadFeedbackManagement() {
    const tbody = document.getElementById('feedbackTableTbody');
    const emptyState = document.getElementById('feedbackEmptyState');
    const tableEl = document.getElementById('feedbackManagementTable');
    const filteredBadge = document.getElementById('fbFilteredCountBadge');
    const ratingFilter = document.getElementById('fbRatingFilter');
    const searchInput = document.getElementById('fbSearchInput');

    const ratingVal = ratingFilter ? ratingFilter.value : '';
    const searchVal = searchInput ? searchInput.value.trim() : '';

    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center">Loading feedback records...</td></tr>';
    }

    try {
        let url = '/api/knowledge/feedback';
        const params = [];
        if (ratingVal) params.push(`rating=${encodeURIComponent(ratingVal)}`);
        if (searchVal) params.push(`search=${encodeURIComponent(searchVal)}`);
        if (params.length > 0) url += '?' + params.join('&');

        const res = await fetch(url);
        if (!res.ok) {
            if (res.status === 403) {
                if (tbody) tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">Access Restricted: Feedback management is reserved for Administrators and HR Managers.</td></tr>';
            } else {
                if (tbody) tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">Unable to load feedback data.</td></tr>';
            }
            return;
        }

        const data = await res.json();
        AppState.feedbackItems = data.items || [];

        // 1. Update KPI Summary Cards
        const kpiTotal = document.getElementById('fbKpiTotalSubmissions');
        const kpiAvg = document.getElementById('fbKpiAverageRating');
        const kpiPositive = document.getElementById('fbKpiPositiveCount');
        const kpiPositiveRatio = document.getElementById('fbKpiPositiveRatio');
        const kpiLow = document.getElementById('fbKpiLowCount');

        if (kpiTotal) kpiTotal.textContent = data.totalSubmissions;
        if (kpiAvg) {
            kpiAvg.textContent = data.hasRatings ? (data.averageRating.toFixed(1) + ' / 5') : '0.0 / 5';
        }

        const count5 = (data.ratingDistribution && data.ratingDistribution[5]) || 0;
        const count4 = (data.ratingDistribution && data.ratingDistribution[4]) || 0;
        const count3 = (data.ratingDistribution && data.ratingDistribution[3]) || 0;
        const count2 = (data.ratingDistribution && data.ratingDistribution[2]) || 0;
        const count1 = (data.ratingDistribution && data.ratingDistribution[1]) || 0;

        const positiveCount = count5 + count4;
        const lowCount = count2 + count1;
        const positiveRatioPct = data.totalSubmissions > 0 ? Math.round((positiveCount / data.totalSubmissions) * 100) : 0;

        if (kpiPositive) kpiPositive.textContent = positiveCount;
        if (kpiPositiveRatio) kpiPositiveRatio.textContent = `${positiveRatioPct}% positive ratings`;
        if (kpiLow) kpiLow.textContent = lowCount;

        // 2. Render 1 to 5 Star Distribution Bars
        const distContainer = document.getElementById('fbDistributionBarsContainer');
        if (distContainer) {
            const starBars = [5, 4, 3, 2, 1].map(star => {
                const count = (data.ratingDistribution && data.ratingDistribution[star]) || 0;
                const pct = data.totalSubmissions > 0 ? Math.round((count / data.totalSubmissions) * 100) : 0;
                let barColor = 'var(--success)';
                if (star === 3) barColor = 'var(--info)';
                else if (star <= 2) barColor = 'var(--warning)';

                return `
                    <div style="display: flex; align-items: center; gap: 0.75rem; font-size: 0.85rem;">
                        <span style="width: 50px; font-weight: 600; color: var(--text-secondary); text-align: right;">${star} ★</span>
                        <div style="flex: 1; height: 10px; background: #e2e8f0; border-radius: 5px; overflow: hidden;">
                            <div style="width: ${pct}%; height: 100%; background: ${barColor}; border-radius: 5px; transition: width 0.3s ease;"></div>
                        </div>
                        <span style="width: 80px; text-align: right; color: var(--text-muted); font-size: 0.82rem;">${count} (${pct}%)</span>
                    </div>
                `;
            }).join('');
            distContainer.innerHTML = starBars;
        }

        // 3. Render Feedback Records Table
        if (filteredBadge) {
            filteredBadge.textContent = `${AppState.feedbackItems.length} Submissions`;
        }

        if (AppState.feedbackItems.length === 0) {
            if (tableEl) tableEl.style.display = 'none';
            if (emptyState) {
                emptyState.style.display = 'block';
                const emptyHeading = document.getElementById('feedbackEmptyHeading');
                const emptySub = document.getElementById('feedbackEmptySub');
                if (ratingVal || searchVal) {
                    if (emptyHeading) emptyHeading.textContent = 'No matching feedback found.';
                    if (emptySub) emptySub.textContent = 'No feedback submissions matched your search query or rating filter. Try clearing filters.';
                } else {
                    if (emptyHeading) emptyHeading.textContent = 'No feedback submitted yet.';
                    if (emptySub) emptySub.textContent = 'As employees ask questions and rate AI answers on the AI Assistant page, persisted records and ratings will appear here.';
                }
            }
        } else {
            if (emptyState) emptyState.style.display = 'none';
            if (tableEl) tableEl.style.display = 'table';
            if (tbody) {
                tbody.innerHTML = AppState.feedbackItems.map(item => {
                    const starsHtml = `<span style="color: #f59e0b; font-size: 0.95rem; letter-spacing: 1px;" title="${item.rating} of 5 Stars">${'★'.repeat(item.rating)}<span style="color: #cbd5e1;">${'★'.repeat(5 - item.rating)}</span></span>`;
                    const dateStr = item.createdAt ? (new Date(item.createdAt).toLocaleDateString() + ' ' + new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })) : '-';
                    const qDisplay = item.question ? (item.question.length > 40 ? escapeHtml(item.question.substring(0, 40)) + '...' : escapeHtml(item.question)) : '-';
                    const cDisplay = item.comment && item.comment.trim() ? (item.comment.length > 40 ? escapeHtml(item.comment.substring(0, 40)) + '...' : escapeHtml(item.comment)) : '<span style="color: var(--text-muted);">—</span>';

                    let docHtml = '<span class="badge badge-mode" style="font-size: 0.72rem;">Direct / Abstention</span>';
                    if (item.documentId) {
                        const titleSnippet = item.documentTitle ? (item.documentTitle.length > 25 ? escapeHtml(item.documentTitle.substring(0, 25)) + '...' : escapeHtml(item.documentTitle)) : 'Document';
                        docHtml = `
                            <div style="display: flex; flex-direction: column; gap: 0.15rem;">
                                <strong style="font-size: 0.85rem;">#${item.documentId} - ${titleSnippet}</strong>
                                <span class="badge badge-employee" style="font-size: 0.68rem; align-self: flex-start;">${escapeHtml(item.documentCategory || 'General')}</span>
                            </div>
                        `;
                    }

                    return `
                        <tr>
                            <td>${starsHtml}</td>
                            <td style="font-size: 0.88rem; max-width: 220px;" title="${escapeHtml(item.question || '')}"><strong>${qDisplay}</strong></td>
                            <td style="font-size: 0.85rem; max-width: 200px;" title="${escapeHtml(item.comment || '')}">${cDisplay}</td>
                            <td>${docHtml}</td>
                            <td><code style="font-size: 0.78rem;">${escapeHtml(item.answerReference || '-')}</code></td>
                            <td style="font-size: 0.85rem;">${escapeHtml(item.userId || 'Anonymous')}</td>
                            <td style="font-size: 0.82rem; color: var(--text-secondary); white-space: nowrap;">${dateStr}</td>
                            <td style="text-align: center;">
                                <button type="button" class="btn btn-secondary btn-sm" onclick="openFeedbackDetails(${item.id})">
                                    🔍 Inspect
                                </button>
                            </td>
                        </tr>
                    `;
                }).join('');
            }
        }

    } catch (err) {
        console.error('Load feedback error:', err);
        if (tbody) tbody.innerHTML = '<tr><td colspan="8" class="text-center">Error loading feedback management data.</td></tr>';
    }
}

function openFeedbackDetails(feedbackId) {
    const item = (AppState.feedbackItems || []).find(f => f.id === feedbackId);
    if (!item) return;

    const modal = document.getElementById('feedbackDetailsModal');
    const starsEl = document.getElementById('fbModalRatingStars');
    const numEl = document.getElementById('fbModalRatingNum');
    const submitterEl = document.getElementById('fbModalSubmitter');
    const dateEl = document.getElementById('fbModalDate');
    const qEl = document.getElementById('fbModalQuestion');
    const cEl = document.getElementById('fbModalComment');
    const answerEl = document.getElementById('fbModalAnswerText');
    const docInfoEl = document.getElementById('fbModalDocInfo');
    const viewDocBtn = document.getElementById('fbModalViewDocBtn');
    const refTag = document.getElementById('fbModalAnswerRefTag');

    if (starsEl) starsEl.textContent = '★'.repeat(item.rating) + '☆'.repeat(5 - item.rating);
    if (numEl) numEl.textContent = `(${item.rating} / 5 Stars)`;
    if (submitterEl) submitterEl.textContent = item.userId ? `Submitted by ${item.userId}` : 'Submitted by Anonymous';
    if (dateEl) dateEl.textContent = item.createdAt ? new Date(item.createdAt).toLocaleString() : '-';
    if (qEl) qEl.textContent = item.question || '-';
    if (cEl) {
        cEl.innerHTML = item.comment && item.comment.trim()
            ? escapeHtml(item.comment)
            : '<span style="color: var(--text-muted); font-style: italic;">No additional comments submitted.</span>';
    }
    if (answerEl) {
        answerEl.innerHTML = item.answerText && item.answerText.trim()
            ? formatRichAiAnswer(item.answerText, [])
            : '<span style="color: var(--text-muted); font-style: italic;">Full answer text was not recorded for this legacy submission.</span>';
    }

    if (item.documentId) {
        if (docInfoEl) {
            docInfoEl.innerHTML = `<strong>#${item.documentId} - ${escapeHtml(item.documentTitle || 'Document')}</strong> &bull; Category: ${escapeHtml(item.documentCategory || 'General')} &bull; Version ${item.documentVersion || 1}`;
        }
        if (viewDocBtn) {
            viewDocBtn.style.display = 'inline-block';
            viewDocBtn.onclick = () => {
                if (modal) modal.style.display = 'none';
                openFullDocumentReader(item.documentId);
            };
        }
    } else {
        if (docInfoEl) {
            docInfoEl.textContent = 'Direct answer or evidence abstention (no company document cited).';
        }
        if (viewDocBtn) viewDocBtn.style.display = 'none';
    }

    if (refTag) refTag.textContent = `Ref: ${item.answerReference || '-'}`;
    if (modal) modal.style.display = 'flex';
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

// ===================================================================
// SCREEN H: AUDIT LOGS (ADMIN ONLY)
// ===================================================================
function initAuditLogs() {
    const refreshBtn = document.getElementById('btnRefreshAuditLogs');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', loadAuditLogs);
    }
}

async function loadAuditLogs() {
    const tbody = document.getElementById('auditLogsTableTbody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="7" class="text-center">Loading security audit events...</td></tr>';

    try {
        const res = await fetch('/api/knowledge/audit-logs');
        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Access Restricted: Only Administrators can inspect audit logs.</td></tr>';
            return;
        }

        const logs = await res.json();
        if (!Array.isArray(logs) || logs.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No audit events recorded yet.</td></tr>';
            return;
        }

        tbody.innerHTML = logs.map(l => {
            const timeStr = l.timestamp ? new Date(l.timestamp).toLocaleString() : '-';
            const isSuccess = l.outcome === 'SUCCESS';
            const outcomeBadgeClass = isSuccess ? 'badge-active' : 'badge-admin';

            return `
                <tr>
                    <td style="font-size: 0.82rem; color: var(--text-secondary); white-space: nowrap;">${timeStr}</td>
                    <td><strong>${escapeHtml(l.actor)}</strong></td>
                    <td><code>${escapeHtml(l.action)}</code></td>
                    <td>${escapeHtml(l.resourceType)}</td>
                    <td>${l.resourceId != null ? '#' + l.resourceId : '-'}</td>
                    <td><span class="badge ${outcomeBadgeClass}">${escapeHtml(l.outcome)}</span></td>
                    <td style="font-size: 0.85rem; max-width: 320px;">${escapeHtml(l.details || '-')}</td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        console.error('Load audit logs error:', err);
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">Error retrieving audit logs.</td></tr>';
    }
}

// ===================================================================
// SCREEN I: PROCESSING JOBS (ADMIN ONLY)
// ===================================================================
function initProcessingJobs() {
    const refreshBtn = document.getElementById('btnRefreshProcessingJobs');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', loadProcessingJobs);
    }
}

async function loadProcessingJobs() {
    const tbody = document.getElementById('processingJobsTableTbody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="10" class="text-center">Loading processing pipeline jobs...</td></tr>';

    try {
        const res = await fetch('/api/knowledge/processing-jobs');
        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="10" class="text-center text-muted">Access Restricted: Only Administrators can inspect processing jobs.</td></tr>';
            return;
        }

        const jobs = await res.json();
        if (!Array.isArray(jobs) || jobs.length === 0) {
            tbody.innerHTML = '<tr><td colspan="10" class="text-center text-muted">No processing jobs recorded in the queue yet.</td></tr>';
            return;
        }

        tbody.innerHTML = jobs.map(j => {
            const startedStr = j.startedAt ? new Date(j.startedAt).toLocaleTimeString() : '-';
            const completedStr = j.completedAt ? new Date(j.completedAt).toLocaleTimeString() : '-';
            let statusBadgeClass = 'badge-success';
            if (j.status === 'FAILED') statusBadgeClass = 'badge-admin';
            else if (j.status === 'PENDING') statusBadgeClass = 'badge-mode';

            const canRetry = j.status === 'FAILED';

            return `
                <tr>
                    <td><strong>#${j.id}</strong></td>
                    <td>${escapeHtml(j.documentTitle || '#' + j.documentId)}</td>
                    <td>v${j.documentVersion}</td>
                    <td><code>${escapeHtml(j.jobType)}</code></td>
                    <td><span class="badge ${statusBadgeClass}">${escapeHtml(j.status)}</span></td>
                    <td>${j.retryCount}</td>
                    <td style="font-size: 0.82rem; white-space: nowrap;">${startedStr}</td>
                    <td style="font-size: 0.82rem; white-space: nowrap;">${completedStr}</td>
                    <td style="font-size: 0.82rem; color: ${j.errorMessage ? 'var(--danger)' : 'var(--text-secondary)'}; max-width: 240px;">
                        ${escapeHtml(j.errorMessage || 'Completed successfully')}
                    </td>
                    <td>
                        ${canRetry ? `
                            <button type="button" class="btn btn-primary btn-sm" onclick="retryProcessingJob(${j.id})">
                                🔄 Retry
                            </button>
                        ` : '<span style="color: var(--text-muted); font-size: 0.82rem;">-</span>'}
                    </td>
                </tr>
            `;
        }).join('');

    } catch (err) {
        console.error('Load processing jobs error:', err);
        tbody.innerHTML = '<tr><td colspan="10" class="text-center">Error loading processing jobs.</td></tr>';
    }
}

async function retryProcessingJob(jobId) {
    if (!confirm(`Are you sure you want to retry document processing job #${jobId}?`)) {
        return;
    }
    try {
        const res = await fetch(`/api/knowledge/processing-jobs/${jobId}/retry`, { method: 'POST' });
        if (res.ok) {
            const job = await res.json();
            if (job.status === 'INDEXED') {
                showAlert(`Processing job #${jobId} successfully completed! Document is re-indexed.`, 'success');
            } else {
                showAlert(`Job #${jobId} retry finished with status: ${job.status}`, 'warning');
            }
            loadProcessingJobs();
            loadDashboardData();
        } else {
            const err = await res.json();
            showAlert(err.message || 'Failed to retry job.', 'danger');
        }
    } catch (err) {
        console.error('Retry error:', err);
    }
}

// ===================================================================
// SCREEN J: SYSTEM SETTINGS (ADMIN ONLY)
// ===================================================================
function initSystemSettings() {
    // Loaded automatically when switching to systemSettingsSection
}

async function loadSystemSettings() {
    const aiBadge = document.getElementById('settingsAiStatusBadge');
    const aiBody = document.getElementById('settingsAiBody');
    const searchBody = document.getElementById('settingsSearchBody');
    const ingestionBody = document.getElementById('settingsIngestionBody');
    const securityBody = document.getElementById('settingsSecurityBody');

    try {
        const res = await fetch('/api/knowledge/system-settings');
        if (!res.ok) {
            if (aiBody) aiBody.innerHTML = '<p class="text-muted">Access Restricted: Only Administrators can inspect system settings.</p>';
            return;
        }

        const s = await res.json();

        // 1. AI Engine
        if (aiBadge) {
            aiBadge.className = s.executionMode === 'REAL_AI' ? 'badge badge-success' : 'badge badge-mode';
            aiBadge.textContent = s.executionMode === 'REAL_AI' ? '✓ Real Generative AI' : 'Deterministic Simulation';
        }
        if (aiBody) {
            aiBody.innerHTML = `
                <div style="display: flex; flex-direction: column; gap: 0.5rem; font-size: 0.88rem;">
                    <div><strong>Configured Provider:</strong> ${escapeHtml(s.aiProvider)}</div>
                    <div><strong>Active Model:</strong> ${escapeHtml(s.aiModelName)}</div>
                    <div><strong>Execution Mode:</strong> <code>${escapeHtml(s.executionMode)}</code></div>
                    <div><strong>External API Key:</strong> ${s.aiApiKeyConfigured ? '✓ Configured securely in backend environment' : 'Not configured (using verified prototype simulation)'}</div>
                </div>
            `;
        }

        // 2. Search Engine
        if (searchBody) {
            searchBody.innerHTML = `
                <div style="display: flex; flex-direction: column; gap: 0.5rem; font-size: 0.88rem;">
                    <div><strong>Retrieval Mode:</strong> ${escapeHtml(s.retrievalMode)}</div>
                    <div><strong>Scoring Weights:</strong> ${escapeHtml(s.retrievalDetails)}</div>
                    <div><strong>Vector Store Fallback:</strong> ${escapeHtml(s.embeddingFallback)}</div>
                </div>
            `;
        }

        // 3. Ingestion & Validation
        if (ingestionBody) {
            ingestionBody.innerHTML = `
                <div style="display: flex; flex-direction: column; gap: 0.5rem; font-size: 0.88rem;">
                    <div><strong>Supported File Types:</strong> ${(s.supportedFormats || []).join(', ')}</div>
                    <div><strong>Max File Size Limit:</strong> ${escapeHtml(s.maxFileSizeDisplay)} (${Number(s.maxFileSizeBytes || 0).toLocaleString()} bytes)</div>
                    <div><strong>Magic-Byte Validation:</strong> ✓ Enforced (header %PDF- & non-binary TXT checks)</div>
                    <div><strong>Storage Directory:</strong> <code>${escapeHtml(s.uploadDirectory)}</code></div>
                </div>
            `;
        }

        // 4. Security
        if (securityBody) {
            securityBody.innerHTML = `
                <div style="display: flex; flex-direction: column; gap: 0.5rem; font-size: 0.88rem;">
                    <div><strong>Enforced Roles:</strong> ${(s.rolesSupported || []).join(', ')}</div>
                    <div><strong>Clearance Tiers:</strong> ${(s.accessLevelsSupported || []).join(', ')}</div>
                    <div><strong>Prompt-Injection Shield:</strong> ${escapeHtml(s.promptInjectionDefense)}</div>
                    <div><strong>Server-Side Authorization:</strong> Pre-retrieval clearance filter active in Java service layer</div>
                </div>
            `;
        }

    } catch (err) {
        console.error('Load system settings error:', err);
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

// ===================================================================
// SCREEN E: EMPLOYEES INFORMATION & LEAVE DIRECTORY (ADMIN & HR)
// ===================================================================
let allEmployeesCache = [];

function initEmployees() {
    const refreshBtn = document.getElementById('btnRefreshEmployees');
    const filterBtn = document.getElementById('btnFilterEmployees');
    const clearBtn = document.getElementById('btnClearEmployeeFilter');
    const deptSelect = document.getElementById('empDepartmentFilter');
    const searchInput = document.getElementById('empSearchInput');
    const btnOpenAdd = document.getElementById('btnOpenAddEmployee');

    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => loadEmployeesInformation());
    }
    if (filterBtn) {
        filterBtn.addEventListener('click', () => applyEmployeeFilters());
    }
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            if (deptSelect) deptSelect.value = 'ALL';
            if (searchInput) searchInput.value = '';
            applyEmployeeFilters();
        });
    }
    if (deptSelect) {
        deptSelect.addEventListener('change', () => applyEmployeeFilters());
    }
    if (searchInput) {
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                applyEmployeeFilters();
            }
        });
    }

    if (btnOpenAdd) {
        btnOpenAdd.addEventListener('click', () => openAddEmployeeModal());
    }

    // Sub-Tabs Navigation for Employee Information Sections
    const subtabBtns = document.querySelectorAll('.emp-subtab-btn');
    subtabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetPanelId = btn.getAttribute('data-emp-tab');
            switchEmployeeSubtab(targetPanelId);
        });
    });

    initAddEmployeeModal();
}

function switchEmployeeSubtab(targetPanelId) {
    if (!targetPanelId) return;
    const panels = document.querySelectorAll('.emp-tab-panel');
    const btns = document.querySelectorAll('.emp-subtab-btn');

    panels.forEach(p => {
        if (p.id === targetPanelId) {
            p.style.display = 'block';
            p.classList.add('active');
        } else {
            p.style.display = 'none';
            p.classList.remove('active');
        }
    });

    btns.forEach(b => {
        if (b.getAttribute('data-emp-tab') === targetPanelId) {
            b.classList.add('active');
        } else {
            b.classList.remove('active');
        }
    });
}

function showAddEmpModalAlert(message, type = 'danger') {
    const alertEl = document.getElementById('addEmpModalAlert');
    const msgEl = document.getElementById('addEmpModalAlertMessage');
    const modalBody = document.querySelector('#addEmployeeModal .modal-body');
    if (alertEl && msgEl) {
        alertEl.className = `alert-banner alert-${type}`;
        alertEl.style.border = type === 'danger' ? '1px solid #fecaca' : '1px solid #a7f3d0';
        alertEl.style.display = 'flex';
        msgEl.textContent = message;
        if (modalBody) {
            modalBody.scrollTop = 0;
        }
    } else {
        showAlert(message, type);
    }
}

function hideAddEmpModalAlert() {
    const alertEl = document.getElementById('addEmpModalAlert');
    if (alertEl) {
        alertEl.style.display = 'none';
    }
}

function highlightAddEmpErrorField(fieldId) {
    clearAddEmpFieldErrors();
    const el = document.getElementById(fieldId);
    if (el) {
        el.style.borderColor = '#ef4444';
        el.style.boxShadow = '0 0 0 3px rgba(239, 68, 68, 0.15)';
        el.focus();
    }
}

function clearAddEmpFieldErrors() {
    const fieldIds = ['newEmpUsername', 'newEmpPassword', 'newEmpFullName', 'newEmpDepartment', 'newEmpDesignation', 'newEmpManager'];
    fieldIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) {
            el.style.borderColor = '';
            el.style.boxShadow = '';
        }
    });
}

function openAddEmployeeModal() {
    const modal = document.getElementById('addEmployeeModal');
    const form = document.getElementById('addEmployeeForm');
    if (form) form.reset();
    hideAddEmpModalAlert();
    clearAddEmpFieldErrors();

    const plannedInput = document.getElementById('newEmpPlanned');
    const casualInput = document.getElementById('newEmpCasual');
    const sickInput = document.getElementById('newEmpSick');
    const wfhInput = document.getElementById('newEmpWfh');
    const compOffInput = document.getElementById('newEmpCompOff');
    const roleSelect = document.getElementById('newEmpRole');

    if (plannedInput) plannedInput.value = '25';
    if (casualInput) casualInput.value = '6';
    if (sickInput) sickInput.value = '10';
    if (wfhInput) wfhInput.value = '24';
    if (compOffInput) compOffInput.value = '0';
    if (roleSelect) roleSelect.value = 'ROLE_EMPLOYEE';

    const emailPrefixInput = document.getElementById('newEmpEmailPrefix');
    const emailHiddenInput = document.getElementById('newEmpEmail');
    if (emailPrefixInput) emailPrefixInput.value = '';
    if (emailHiddenInput) emailHiddenInput.value = '';
    window._addEmpEmailPrefixEdited = false;

    if (modal) modal.style.display = 'flex';
    const userField = document.getElementById('newEmpUsername');
    if (userField) setTimeout(() => userField.focus(), 60);
}

function closeAddEmployeeModal() {
    const modal = document.getElementById('addEmployeeModal');
    if (modal) modal.style.display = 'none';
    hideAddEmpModalAlert();
    clearAddEmpFieldErrors();
    window._addEmpEmailPrefixEdited = false;
}

function initAddEmployeeModal() {
    const modal = document.getElementById('addEmployeeModal');
    const closeBtn = document.getElementById('btnAddEmpModalClose');
    const cancelBtn = document.getElementById('btnAddEmpModalCancel');
    const form = document.getElementById('addEmployeeForm');
    const usernameInput = document.getElementById('newEmpUsername');
    const emailPrefixInput = document.getElementById('newEmpEmailPrefix');
    const emailHiddenInput = document.getElementById('newEmpEmail');
    const alertCloseBtn = document.getElementById('addEmpModalAlertClose');

    if (closeBtn) closeBtn.addEventListener('click', closeAddEmployeeModal);
    if (cancelBtn) cancelBtn.addEventListener('click', closeAddEmployeeModal);
    if (alertCloseBtn) alertCloseBtn.addEventListener('click', hideAddEmpModalAlert);

    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeAddEmployeeModal();
        });
    }

    if (emailPrefixInput) {
        emailPrefixInput.addEventListener('input', () => {
            window._addEmpEmailPrefixEdited = emailPrefixInput.value.trim().length > 0;
            const cleaned = emailPrefixInput.value.toLowerCase().replace(/[^a-z0-9._-]/g, '');
            if (emailPrefixInput.value !== cleaned) {
                emailPrefixInput.value = cleaned;
            }
            if (emailHiddenInput) {
                emailHiddenInput.value = cleaned ? `${cleaned}@company.internal` : '';
            }
        });
    }

    if (usernameInput) {
        usernameInput.addEventListener('input', () => {
            const orig = usernameInput.value;
            const lower = orig.toLowerCase().replace(/\s+/g, '');
            if (orig !== lower) {
                usernameInput.value = lower;
            }
            if (!window._addEmpEmailPrefixEdited && emailPrefixInput) {
                const cleanUser = lower.trim().replace(/[^a-z0-9._-]/g, '');
                emailPrefixInput.value = cleanUser;
                if (emailHiddenInput) {
                    emailHiddenInput.value = cleanUser ? `${cleanUser}@company.internal` : '';
                }
            }
        });
    }

    if (form) {
        form.addEventListener('input', () => {
            hideAddEmpModalAlert();
            clearAddEmpFieldErrors();
        });

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const submitBtn = document.getElementById('btnAddEmpSubmit');
            const originalBtnText = submitBtn ? submitBtn.innerHTML : 'Save';

            const username = document.getElementById('newEmpUsername')?.value.trim();
            const password = document.getElementById('newEmpPassword')?.value;
            const role = document.getElementById('newEmpRole')?.value || 'ROLE_EMPLOYEE';
            const fullName = document.getElementById('newEmpFullName')?.value.trim();
            const emailPrefix = document.getElementById('newEmpEmailPrefix')?.value.trim().toLowerCase();
            const email = emailPrefix ? `${emailPrefix}@company.internal` : (document.getElementById('newEmpEmail')?.value.trim() || '');
            const employeeCode = document.getElementById('newEmpCode')?.value.trim();
            const department = document.getElementById('newEmpDepartment')?.value;
            const designation = document.getElementById('newEmpDesignation')?.value.trim();
            const location = document.getElementById('newEmpLocation')?.value.trim();
            const reportingManager = document.getElementById('newEmpManager')?.value.trim();

            const plannedLeavesTotal = parseInt(document.getElementById('newEmpPlanned')?.value || '25', 10);
            const casualLeavesTotal = parseInt(document.getElementById('newEmpCasual')?.value || '6', 10);
            const sickLeavesTotal = parseInt(document.getElementById('newEmpSick')?.value || '10', 10);
            const wfhAllowanceDays = parseInt(document.getElementById('newEmpWfh')?.value || '24', 10);
            const compOffBalance = parseInt(document.getElementById('newEmpCompOff')?.value || '0', 10);

            // 1. Basic Required Fields Check
            if (!username || !password || !fullName || !department || !designation || !emailPrefix) {
                showAddEmpModalAlert('Please fill in all required fields marked with *', 'danger');
                if (!username) highlightAddEmpErrorField('newEmpUsername');
                else if (!password) highlightAddEmpErrorField('newEmpPassword');
                else if (!fullName) highlightAddEmpErrorField('newEmpFullName');
                else if (!emailPrefix) highlightAddEmpErrorField('newEmpEmailPrefix');
                else if (!department) highlightAddEmpErrorField('newEmpDepartment');
                else if (!designation) highlightAddEmpErrorField('newEmpDesignation');
                return;
            }

            // 2. Full Name Validation (No numbers, alphabetic characters only, min 2 letters)
            if (/\d/.test(fullName)) {
                showAddEmpModalAlert('Full Name cannot contain numbers. Only alphabetic letters, spaces, and valid name characters are permitted.', 'danger');
                highlightAddEmpErrorField('newEmpFullName');
                return;
            }
            if (!/^(?=.*[a-zA-Z].*[a-zA-Z])[a-zA-Z\s.'-]+$/.test(fullName)) {
                showAddEmpModalAlert('Full Name must contain at least 2 alphabetic letters and cannot be numbers or symbols only.', 'danger');
                highlightAddEmpErrorField('newEmpFullName');
                return;
            }

            // 3. Username Validation (Lowercase alphanumeric, starts with letter, no spaces or numbers-only)
            if (/[A-Z]/.test(username)) {
                showAddEmpModalAlert('Username must be lowercase letters only (e.g. rahul.sharma).', 'danger');
                highlightAddEmpErrorField('newEmpUsername');
                return;
            }
            if (/^\d+$/.test(username) || !/^[a-z]/.test(username)) {
                showAddEmpModalAlert('Username must start with a lowercase letter and cannot be numbers only.', 'danger');
                highlightAddEmpErrorField('newEmpUsername');
                return;
            }
            if (!/^[a-z][a-z0-9._]{2,29}$/.test(username)) {
                showAddEmpModalAlert('Username must be 3-30 characters long, starting with a lowercase letter, and contain only lowercase letters, digits, dots or underscores.', 'danger');
                highlightAddEmpErrorField('newEmpUsername');
                return;
            }

            // 4. Password Validation (Min 6 characters, mixed letters and numbers/symbols)
            if (password.length < 6) {
                showAddEmpModalAlert('Initial Password must be at least 6 characters long.', 'danger');
                highlightAddEmpErrorField('newEmpPassword');
                return;
            }
            const hasLetter = /[a-zA-Z]/.test(password);
            const hasNumberOrSymbol = /[0-9!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password);
            if (!hasLetter || !hasNumberOrSymbol) {
                showAddEmpModalAlert('Initial Password must contain a mix of letters and numbers/symbols (e.g. EmployeePass2026!). Passwords with only numbers are not permitted.', 'danger');
                highlightAddEmpErrorField('newEmpPassword');
                return;
            }

            // 5. Corporate Email Validation
            if (!emailPrefix) {
                showAddEmpModalAlert('Corporate Email prefix is required.', 'danger');
                highlightAddEmpErrorField('newEmpEmailPrefix');
                return;
            }
            if (!/^[a-z][a-z0-9._-]{1,29}$/.test(emailPrefix)) {
                showAddEmpModalAlert('Corporate Email prefix must start with a lowercase letter and contain only lowercase letters, digits, dots, or underscores (2-30 characters).', 'danger');
                highlightAddEmpErrorField('newEmpEmailPrefix');
                return;
            }
            if (!/^[a-z0-9._-]+@company\.internal$/.test(email)) {
                showAddEmpModalAlert('Corporate Email must follow the official company format: <username>@company.internal', 'danger');
                highlightAddEmpErrorField('newEmpEmailPrefix');
                return;
            }

            // 6. Reporting Manager Validation (if provided, must not contain numbers)
            if (reportingManager) {
                if (/\d/.test(reportingManager)) {
                    showAddEmpModalAlert('Reporting Manager cannot contain numbers. Only letters and standard name characters are permitted.', 'danger');
                    highlightAddEmpErrorField('newEmpManager');
                    return;
                }
                if (!/^[a-zA-Z\s.'-]+$/.test(reportingManager)) {
                    showAddEmpModalAlert('Reporting Manager contains invalid characters. Only letters and standard name characters are allowed.', 'danger');
                    highlightAddEmpErrorField('newEmpManager');
                    return;
                }
            }

            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<span class="spinner" style="width: 14px; height: 14px; display: inline-block; vertical-align: middle; margin-right: 0.35rem;"></span> Creating...';
            }

            try {
                const res = await fetch('/api/employees', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        username,
                        password,
                        role,
                        fullName,
                        email,
                        employeeCode,
                        department,
                        designation,
                        location,
                        reportingManager,
                        plannedLeavesTotal,
                        casualLeavesTotal,
                        sickLeavesTotal,
                        wfhAllowanceDays,
                        compOffBalance
                    })
                });

                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = originalBtnText;
                }

                if (!res.ok) {
                    const err = await res.json().catch(() => ({}));
                    const errMsg = err.message || 'Failed to create employee account.';
                    showAddEmpModalAlert(errMsg, 'danger');
                    if (errMsg.toLowerCase().includes('username')) highlightAddEmpErrorField('newEmpUsername');
                    else if (errMsg.toLowerCase().includes('password')) highlightAddEmpErrorField('newEmpPassword');
                    else if (errMsg.toLowerCase().includes('email')) highlightAddEmpErrorField('newEmpEmailPrefix');
                    else if (errMsg.toLowerCase().includes('name')) highlightAddEmpErrorField('newEmpFullName');
                    return;
                }

                const created = await res.json();
                closeAddEmployeeModal();
                form.reset();
                window._addEmpEmailPrefixEdited = false;
                showAlert(`Employee '${created.fullName}' (@${created.username}) created successfully! They can immediately log into the portal.`, 'success');
                await loadEmployeesInformation();

            } catch (err) {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = originalBtnText;
                }
                console.error('Error creating employee:', err);
                showAddEmpModalAlert('An unexpected network error occurred while creating the employee.', 'danger');
            }
        });
    }
}

async function loadEmployeesInformation() {
    const tbody = document.getElementById('employeeLeavesTbody');
    const profilesGrid = document.getElementById('employeeProfilesGrid');
    const requestsTbody = document.getElementById('employeeLeaveRequestsTbody');

    if (tbody) tbody.innerHTML = '<tr><td colspan="9" class="text-center"><div class="spinner" style="margin: 1rem auto;"></div>Loading employee data...</td></tr>';
    if (profilesGrid) profilesGrid.innerHTML = '<div style="padding: 2rem; text-align: center; grid-column: 1/-1;"><div class="spinner" style="margin: 0 auto 1rem;"></div>Loading employee profiles...</div>';
    if (requestsTbody) requestsTbody.innerHTML = '<tr><td colspan="6" class="text-center">Loading leave applications...</td></tr>';

    try {
        const res = await fetch('/api/employees');
        if (!res.ok) {
            if (res.status === 403) {
                if (tbody) tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted">Access Restricted: Only Administrators and HR can view employee information.</td></tr>';
            } else {
                if (tbody) tbody.innerHTML = '<tr><td colspan="9" class="text-center">Failed to load employee records.</td></tr>';
            }
            return;
        }

        const data = await res.json();
        allEmployeesCache = data.employees || [];

        // Update KPIs
        const totalCountEl = document.getElementById('empKpiTotalCount');
        const approvedLeavesEl = document.getElementById('empKpiApprovedLeaves');
        const pendingReqsEl = document.getElementById('empKpiPendingRequests');
        const wfhDaysEl = document.getElementById('empKpiWfhDaysUsed');

        if (totalCountEl) totalCountEl.textContent = data.totalEmployees ?? allEmployeesCache.length;
        if (approvedLeavesEl) approvedLeavesEl.textContent = data.totalApprovedLeaves ?? 0;
        if (pendingReqsEl) pendingReqsEl.textContent = data.totalPendingRequests ?? 0;
        if (wfhDaysEl) wfhDaysEl.textContent = data.totalWfhDaysUsed ?? 0;

        updateDepartmentFilterOptions(data.departmentCounts, allEmployeesCache);
        applyEmployeeFilters();

    } catch (err) {
        console.error('Error loading employees overview:', err);
        if (tbody) tbody.innerHTML = '<tr><td colspan="9" class="text-center text-danger">Error communicating with employee service.</td></tr>';
    }
}

function updateDepartmentFilterOptions(deptCounts = {}, employees = []) {
    const deptSelect = document.getElementById('empDepartmentFilter');
    if (!deptSelect) return;

    const currentVal = deptSelect.value || 'ALL';

    const standardDepts = [
        'Engineering',
        'Human Resources',
        'Administration',
        'Finance',
        'Product',
        'Marketing',
        'Operations',
        'Sales',
        'Legal',
        'Support'
    ];

    const allDepts = new Set(standardDepts);
    Object.keys(deptCounts || {}).forEach(d => {
        if (d && d.trim()) allDepts.add(d.trim());
    });
    employees.forEach(e => {
        if (e.department && e.department.trim()) allDepts.add(e.department.trim());
    });

    let html = `<option value="ALL">All Departments (${employees.length})</option>`;
    Array.from(allDepts).sort().forEach(dept => {
        const count = (deptCounts && deptCounts[dept] !== undefined)
            ? deptCounts[dept]
            : employees.filter(e => (e.department || '').toLowerCase() === dept.toLowerCase()).length;
        const countLabel = count > 0 ? ` (${count})` : '';
        html += `<option value="${escapeHtml(dept)}">${escapeHtml(dept)}${countLabel}</option>`;
    });

    deptSelect.innerHTML = html;
    if (currentVal && (currentVal === 'ALL' || Array.from(allDepts).some(d => d.toLowerCase() === currentVal.toLowerCase()))) {
        deptSelect.value = currentVal;
    } else {
        deptSelect.value = 'ALL';
    }
}

function applyEmployeeFilters() {
    const deptSelect = document.getElementById('empDepartmentFilter');
    const searchInput = document.getElementById('empSearchInput');

    const selectedDept = deptSelect ? deptSelect.value : 'ALL';
    const keyword = searchInput ? searchInput.value.trim().toLowerCase() : '';

    let filtered = allEmployeesCache;

    if (selectedDept && selectedDept !== 'ALL') {
        filtered = filtered.filter(e => (e.department || '').toLowerCase() === selectedDept.toLowerCase());
    }

    if (keyword) {
        filtered = filtered.filter(e => {
            const nameMatch = (e.fullName || '').toLowerCase().includes(keyword);
            const userMatch = (e.username || '').toLowerCase().includes(keyword);
            const codeMatch = (e.employeeCode || '').toLowerCase().includes(keyword);
            const desigMatch = (e.designation || '').toLowerCase().includes(keyword);
            const deptMatch = (e.department || '').toLowerCase().includes(keyword);
            return nameMatch || userMatch || codeMatch || desigMatch || deptMatch;
        });
    }

    renderEmployeeLeavesTable(filtered);
    renderEmployeeProfilesGrid(filtered);
    renderRecentLeaveApplications(filtered);
}

function renderEmployeeLeavesTable(employees) {
    const tbody = document.getElementById('employeeLeavesTbody');
    const badge = document.getElementById('empLeavesTableBadge');
    const tabBadge = document.getElementById('empTabLeavesBadge');

    if (badge) badge.textContent = `${employees.length} Records`;
    if (tabBadge) tabBadge.textContent = employees.length;
    if (!tbody) return;

    if (employees.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center text-muted" style="padding: 2.5rem;">No matching employee records found.</td></tr>';
        return;
    }

    tbody.innerHTML = employees.map(emp => {
        const avatarLetter = (emp.fullName || emp.username || 'E').charAt(0).toUpperCase();
        let deptClass = '';
        if (emp.department === 'Human Resources') deptClass = 'dept-hr';
        else if (emp.department === 'Administration') deptClass = 'dept-admin';

        return `
            <tr>
                <td>
                    <div style="display: flex; align-items: center; gap: 0.75rem;">
                        <div class="emp-avatar-circle ${deptClass}" style="width: 38px; height: 38px; font-size: 0.95rem; font-weight: 700;">
                            ${escapeHtml(avatarLetter)}
                        </div>
                        <div>
                            <strong style="display: block; color: var(--text-primary); font-size: 0.95rem;">${escapeHtml(emp.fullName)}</strong>
                            <span style="font-size: 0.76rem; color: var(--text-muted); font-family: monospace;">${escapeHtml(emp.employeeCode)} • @${escapeHtml(emp.username)}</span>
                        </div>
                    </div>
                </td>
                <td>
                    <span class="badge ${deptClass === 'dept-hr' ? 'badge-success' : deptClass === 'dept-admin' ? 'badge-admin' : 'badge-primary'}">${escapeHtml(emp.department)}</span>
                    <div style="font-size: 0.8rem; color: var(--text-secondary); margin-top: 3px;">${escapeHtml(emp.designation)}</div>
                </td>
                <td class="text-center">
                    <span class="leave-pill-stat pill-blue" title="${emp.plannedLeavesTaken} days taken of ${emp.plannedLeavesTotal} total">
                        <strong>${emp.plannedLeavesRemaining}</strong> / ${emp.plannedLeavesTotal} <span class="pill-unit">left</span>
                    </span>
                </td>
                <td class="text-center">
                    <span class="leave-pill-stat pill-green" title="${emp.casualLeavesTaken} days taken of ${emp.casualLeavesTotal} total">
                        <strong>${emp.casualLeavesRemaining}</strong> / ${emp.casualLeavesTotal} <span class="pill-unit">left</span>
                    </span>
                </td>
                <td class="text-center">
                    <span class="leave-pill-stat pill-purple" title="${emp.sickLeavesTaken} days taken of ${emp.sickLeavesTotal} total">
                        <strong>${emp.sickLeavesRemaining}</strong> / ${emp.sickLeavesTotal} <span class="pill-unit">left</span>
                    </span>
                </td>
                <td class="text-center">
                    <span class="leave-pill-stat pill-amber" title="${emp.wfhAllowanceDays} allowance, ${emp.wfhUsedDays} used">
                        <strong>${emp.wfhUsedDays}</strong> / ${emp.wfhAllowanceDays} <span class="pill-unit">used</span>
                    </span>
                </td>
                <td class="text-center">
                    <span class="leave-pill-stat pill-indigo" title="${emp.compOffEarned} earned, ${emp.compOffAvailed} availed">
                        <strong>${emp.compOffBalance}</strong> <span class="pill-unit">days bal</span>
                    </span>
                </td>
                <td class="text-center">
                    <div class="leave-status-stack">
                        <span class="status-chip status-chip-success">✓ ${emp.approvedLeavesCount} Approved</span>
                        ${emp.pendingLeavesCount > 0 
                            ? `<span class="status-chip status-chip-warning">⏳ ${emp.pendingLeavesCount} Pending</span>` 
                            : '<span class="status-chip-muted">0 pending</span>'}
                    </div>
                </td>
                <td class="text-center">
                    <button type="button" class="btn btn-secondary btn-sm" onclick="openEmployeeModal('${escapeHtml(emp.username)}')">
                        👁️ View Details
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderEmployeeProfilesGrid(employees) {
    const grid = document.getElementById('employeeProfilesGrid');
    const tabBadge = document.getElementById('empTabDirectoryBadge');
    if (tabBadge) tabBadge.textContent = employees.length;
    if (!grid) return;

    if (employees.length === 0) {
        grid.innerHTML = '<div style="grid-column: 1/-1; padding: 2rem; text-align: center; color: var(--text-muted);">No employee profiles found matching current filters.</div>';
        return;
    }

    grid.innerHTML = employees.map(emp => {
        const avatarLetter = (emp.fullName || emp.username || 'E').charAt(0).toUpperCase();
        let deptClass = '';
        if (emp.department === 'Human Resources') deptClass = 'dept-hr';
        else if (emp.department === 'Administration') deptClass = 'dept-admin';

        return `
            <div class="employee-card">
                <div>
                    <div class="emp-card-header">
                        <div class="emp-avatar-circle ${deptClass}">
                            ${escapeHtml(avatarLetter)}
                        </div>
                        <div style="flex: 1; min-width: 0;">
                            <h4 class="emp-card-title">${escapeHtml(emp.fullName)}</h4>
                            <span class="emp-card-code">${escapeHtml(emp.employeeCode)} • ${escapeHtml(emp.designation)}</span>
                        </div>
                    </div>
                    <div class="emp-card-details">
                        <div class="emp-detail-row">
                            <span class="emp-detail-label">Department</span>
                            <span class="emp-detail-val">${escapeHtml(emp.department)}</span>
                        </div>
                        <div class="emp-detail-row">
                            <span class="emp-detail-label">Email</span>
                            <a href="mailto:${escapeHtml(emp.email)}" style="font-size: 0.82rem; color: var(--primary); text-decoration: none; word-break: break-all;">${escapeHtml(emp.email)}</a>
                        </div>
                        <div class="emp-detail-row">
                            <span class="emp-detail-label">Work Location</span>
                            <span class="emp-detail-val">${escapeHtml(emp.location || 'HQ')}</span>
                        </div>
                        <div class="emp-detail-row">
                            <span class="emp-detail-label">Joined</span>
                            <span class="emp-detail-val">${escapeHtml(emp.dateOfJoining || 'N/A')}</span>
                        </div>
                        <div class="emp-detail-row">
                            <span class="emp-detail-label">Reporting Manager</span>
                            <span class="emp-detail-val">${escapeHtml(emp.reportingManager || 'N/A')}</span>
                        </div>
                        <div class="emp-detail-row">
                            <span class="emp-detail-label">Clearance Tier</span>
                            <span class="badge ${emp.clearanceLevel === 'TIER_3' ? 'badge-admin' : 'badge-employee'}" style="font-size: 0.75rem;">${escapeHtml(emp.clearanceLevel || 'TIER_1')}</span>
                        </div>
                    </div>
                </div>
                <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #f1f5f9; padding-top: 0.75rem; margin-top: 0.5rem;">
                    <div style="font-size: 0.78rem; color: var(--text-muted);">
                        <strong>${emp.plannedLeavesRemaining + emp.casualLeavesRemaining}</strong> total leaves left
                    </div>
                    <button type="button" class="btn btn-secondary btn-sm" onclick="openEmployeeModal('${escapeHtml(emp.username)}')">
                        Full Record &rarr;
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

function renderRecentLeaveApplications(employees) {
    const tbody = document.getElementById('employeeLeaveRequestsTbody');
    const badge = document.getElementById('empApplicationsCountBadge');
    if (!tbody) return;

    // Collect all leave requests across the filtered employees
    const allRequests = [];
    employees.forEach(emp => {
        if (emp.recentLeaveRequests && emp.recentLeaveRequests.length > 0) {
            emp.recentLeaveRequests.forEach(req => {
                allRequests.push({
                    employeeName: emp.fullName,
                    employeeCode: emp.employeeCode,
                    department: emp.department,
                    username: emp.username,
                    ...req
                });
            });
        }
    });

    allRequests.sort((a, b) => (b.id || 0) - (a.id || 0));

    if (badge) badge.textContent = `${allRequests.length} Applications`;
    const tabBadge = document.getElementById('empTabApplicationsBadge');
    if (tabBadge) tabBadge.textContent = allRequests.length;

    if (allRequests.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted" style="padding: 2rem;">No leave applications found for selected employees.</td></tr>';
        return;
    }

    tbody.innerHTML = allRequests.map(req => {
        let statusBadge = '<span class="badge badge-success">✓ Approved</span>';
        if (req.status === 'PENDING') {
            statusBadge = '<span class="badge badge-warning">⏳ Pending Review</span>';
        } else if (req.status === 'REJECTED') {
            statusBadge = '<span class="badge badge-danger">✕ Rejected</span>';
        }

        const lt = (req.leaveType || '').toUpperCase();
        let typeBadgeClass = 'leave-pill-blue';
        if (lt.includes('CASUAL')) typeBadgeClass = 'leave-pill-green';
        else if (lt.includes('SICK')) typeBadgeClass = 'leave-pill-purple';
        else if (lt.includes('WFH') || lt.includes('HOME')) typeBadgeClass = 'leave-pill-amber';
        else if (lt.includes('PLANNED') || lt.includes('ANNUAL')) typeBadgeClass = 'leave-pill-blue';

        const dateRange = (req.startDate === req.endDate || !req.endDate)
            ? req.startDate
            : `${req.startDate} to ${req.endDate}`;

        return `
            <tr>
                <td>
                    <div style="font-weight: 600; color: var(--text-primary);">${escapeHtml(req.employeeName)}</div>
                    <div style="font-size: 0.78rem; color: var(--text-muted); font-family: monospace;">${escapeHtml(req.employeeCode)} • ${escapeHtml(req.department)}</div>
                </td>
                <td>
                    <span class="leave-pill ${typeBadgeClass}">${escapeHtml(req.leaveType)}</span>
                </td>
                <td style="font-size: 0.88rem; font-family: monospace;">
                    ${escapeHtml(dateRange)}
                </td>
                <td>
                    <strong>${req.daysCount}</strong> ${req.daysCount === 1 ? 'day' : 'days'}
                </td>
                <td style="font-size: 0.88rem; color: var(--text-secondary); max-width: 250px;">
                    ${escapeHtml(req.reason || '-')}
                </td>
                <td>
                    ${statusBadge}
                </td>
            </tr>
        `;
    }).join('');
}

async function openEmployeeModal(username) {
    const modal = document.getElementById('employeeDetailsModal');
    const titleEl = document.getElementById('empModalTitle');
    const subtitleEl = document.getElementById('empModalSubtitle');
    const avatarEl = document.getElementById('empModalAvatar');
    const bodyEl = document.getElementById('empModalBody');

    if (!modal) return;

    if (titleEl) titleEl.textContent = 'Loading employee record...';
    if (subtitleEl) subtitleEl.textContent = 'Fetching leave breakdown and company profile...';
    if (bodyEl) bodyEl.innerHTML = '<div style="padding: 3rem; text-align: center;"><div class="spinner" style="margin: 0 auto 1rem;"></div>Loading employee details...</div>';
    modal.style.display = 'flex';

    try {
        const res = await fetch(`/api/employees/${username}`);
        if (!res.ok) {
            if (bodyEl) bodyEl.innerHTML = '<div class="alert alert-danger" style="padding: 1rem; border-radius: 6px;">Unable to load employee details or access is restricted.</div>';
            return;
        }

        const emp = await res.json();

        if (titleEl) titleEl.textContent = `${emp.fullName} (${emp.employeeCode})`;
        if (subtitleEl) subtitleEl.textContent = `${emp.designation} • ${emp.department} • Reporting Manager: ${emp.reportingManager || 'N/A'}`;
        if (avatarEl) avatarEl.textContent = (emp.fullName || 'E').charAt(0).toUpperCase();

        const plannedPercent = emp.plannedLeavesTotal > 0 ? Math.min(100, Math.round((emp.plannedLeavesRemaining / emp.plannedLeavesTotal) * 100)) : 0;
        const casualPercent = emp.casualLeavesTotal > 0 ? Math.min(100, Math.round((emp.casualLeavesRemaining / emp.casualLeavesTotal) * 100)) : 0;
        const sickPercent = emp.sickLeavesTotal > 0 ? Math.min(100, Math.round((emp.sickLeavesRemaining / emp.sickLeavesTotal) * 100)) : 0;
        const wfhPercent = emp.wfhAllowanceDays > 0 ? Math.min(100, Math.round((emp.wfhUsedDays / emp.wfhAllowanceDays) * 100)) : 0;

        let requestsHtml = '<tr><td colspan="5" class="text-center text-muted">No leave requests found.</td></tr>';
        if (emp.recentLeaveRequests && emp.recentLeaveRequests.length > 0) {
            requestsHtml = emp.recentLeaveRequests.map(r => {
                let st = '<span class="badge badge-success">✓ Approved</span>';
                if (r.status === 'PENDING') st = '<span class="badge badge-warning">⏳ Pending</span>';
                else if (r.status === 'REJECTED') st = '<span class="badge badge-danger">✕ Rejected</span>';
                return `
                    <tr>
                        <td><strong>${escapeHtml(r.leaveType)}</strong></td>
                        <td>${escapeHtml(r.startDate)} to ${escapeHtml(r.endDate)}</td>
                        <td>${r.daysCount} d</td>
                        <td>${escapeHtml(r.reason || '-')}</td>
                        <td>${st}</td>
                    </tr>
                `;
            }).join('');
        }

        if (bodyEl) {
            bodyEl.innerHTML = `
                <!-- Profile Summary Grid -->
                <div class="emp-modal-summary-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; padding: 1rem; border-radius: var(--radius-md); border: 1px solid var(--border-color); margin-bottom: 1.5rem;">
                    <div>
                        <span style="font-size: 0.78rem; color: var(--text-muted); display: block;">Official Email</span>
                        <a href="mailto:${escapeHtml(emp.email)}" style="font-weight: 600; color: var(--primary); text-decoration: none;">${escapeHtml(emp.email)}</a>
                    </div>
                    <div>
                        <span style="font-size: 0.78rem; color: var(--text-muted); display: block;">Work Location</span>
                        <span style="font-weight: 600; color: var(--text-primary);">${escapeHtml(emp.location || 'HQ')}</span>
                    </div>
                    <div>
                        <span style="font-size: 0.78rem; color: var(--text-muted); display: block;">Date of Joining</span>
                        <span style="font-weight: 600; color: var(--text-primary);">${escapeHtml(emp.dateOfJoining || 'N/A')}</span>
                    </div>
                    <div>
                        <span style="font-size: 0.78rem; color: var(--text-muted); display: block;">Clearance Tier</span>
                        <span class="badge badge-employee">${escapeHtml(emp.clearanceLevel || 'TIER_1')}</span>
                    </div>
                </div>

                <!-- Leave Balances Progress Cards -->
                <h4 style="font-size: 1rem; margin-bottom: 0.85rem; color: var(--text-primary);">Company Leave & Work Allowance Balances</h4>
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1rem; margin-bottom: 1.5rem;">
                    
                    <!-- Planned Leaves -->
                    <div class="emp-modal-balance-card">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem;">
                            <span style="font-size: 0.85rem; font-weight: 600; color: var(--text-secondary);">🏖️ Planned / Annual</span>
                            <span class="leave-pill leave-pill-blue">${emp.plannedLeavesRemaining} left</span>
                        </div>
                        <div style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary);">${emp.plannedLeavesRemaining} <span style="font-size: 0.8rem; font-weight: 400; color: var(--text-muted);">/ ${emp.plannedLeavesTotal} total</span></div>
                        <div class="leave-meter-wrap">
                            <div class="leave-meter-fill" style="width: ${plannedPercent}%; background: #3b82f6;"></div>
                        </div>
                        <div style="font-size: 0.78rem; color: var(--text-muted); margin-top: 0.35rem;">${emp.plannedLeavesTaken} days availed in 2026</div>
                    </div>

                    <!-- Casual Leaves -->
                    <div class="emp-modal-balance-card">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem;">
                            <span style="font-size: 0.85rem; font-weight: 600; color: var(--text-secondary);">📅 Casual Leaves</span>
                            <span class="leave-pill leave-pill-green">${emp.casualLeavesRemaining} left</span>
                        </div>
                        <div style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary);">${emp.casualLeavesRemaining} <span style="font-size: 0.8rem; font-weight: 400; color: var(--text-muted);">/ ${emp.casualLeavesTotal} total</span></div>
                        <div class="leave-meter-wrap">
                            <div class="leave-meter-fill" style="width: ${casualPercent}%; background: #10b981;"></div>
                        </div>
                        <div style="font-size: 0.78rem; color: var(--text-muted); margin-top: 0.35rem;">${emp.casualLeavesTaken} days taken</div>
                    </div>

                    <!-- Sick Leaves -->
                    <div class="emp-modal-balance-card">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem;">
                            <span style="font-size: 0.85rem; font-weight: 600; color: var(--text-secondary);">🩺 Sick Leaves</span>
                            <span class="leave-pill leave-pill-purple">${emp.sickLeavesRemaining} left</span>
                        </div>
                        <div style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary);">${emp.sickLeavesRemaining} <span style="font-size: 0.8rem; font-weight: 400; color: var(--text-muted);">/ ${emp.sickLeavesTotal} total</span></div>
                        <div class="leave-meter-wrap">
                            <div class="leave-meter-fill" style="width: ${sickPercent}%; background: #8b5cf6;"></div>
                        </div>
                        <div style="font-size: 0.78rem; color: var(--text-muted); margin-top: 0.35rem;">${emp.sickLeavesTaken} days used</div>
                    </div>

                    <!-- WFH Allowance -->
                    <div class="emp-modal-balance-card">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem;">
                            <span style="font-size: 0.85rem; font-weight: 600; color: var(--text-secondary);">🏡 Work From Home</span>
                            <span class="leave-pill leave-pill-amber">${emp.wfhRemainingDays} left</span>
                        </div>
                        <div style="font-size: 1.25rem; font-weight: 700; color: var(--text-primary);">${emp.wfhUsedDays} <span style="font-size: 0.8rem; font-weight: 400; color: var(--text-muted);">/ ${emp.wfhAllowanceDays} allowance</span></div>
                        <div class="leave-meter-wrap">
                            <div class="leave-meter-fill" style="width: ${wfhPercent}%; background: #f59e0b;"></div>
                        </div>
                        <div style="font-size: 0.78rem; color: var(--text-muted); margin-top: 0.35rem;">Remote work policy quota</div>
                    </div>

                    <!-- Comp Off & Request Stats -->
                    <div class="emp-modal-balance-card" style="grid-column: 1/-1;">
                        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
                            <div>
                                <span style="font-size: 0.85rem; font-weight: 600; color: var(--text-secondary);">⚡ Compensatory Off (Comp Off):</span>
                                <span style="font-weight: 700; margin-left: 0.5rem; color: var(--primary);">${emp.compOffBalance} Balance</span>
                                <span style="font-size: 0.8rem; color: var(--text-muted); margin-left: 0.5rem;">(${emp.compOffEarned} earned, ${emp.compOffAvailed} availed)</span>
                            </div>
                            <div style="display: flex; gap: 0.5rem;">
                                <span class="badge badge-success">✓ ${emp.approvedLeavesCount} Approved Leaves</span>
                                <span class="badge ${emp.pendingLeavesCount > 0 ? 'badge-warning' : 'badge-pill'}" style="${emp.pendingLeavesCount > 0 ? 'background:#fffbeb; color:#b45309; border:1px solid #fde68a;' : ''}">⏳ ${emp.pendingLeavesCount} Pending</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Leave Requests History -->
                <h4 style="font-size: 1rem; margin-bottom: 0.85rem; color: var(--text-primary);">Leave & WFH Applications Log</h4>
                <div class="table-responsive">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Type</th>
                                <th>Duration</th>
                                <th>Days</th>
                                <th>Reason</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${requestsHtml}
                        </tbody>
                    </table>
                </div>
            `;
        }

    } catch (err) {
        console.error('Error fetching employee detail modal:', err);
        if (bodyEl) bodyEl.innerHTML = '<div class="alert alert-danger">Error retrieving employee details.</div>';
    }
}

// Expose modal handler globally for inline onclick triggers
window.openEmployeeModal = openEmployeeModal;

