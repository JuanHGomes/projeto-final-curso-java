// Instâncias globais
let accountManager;
let sseManager;
let notificationUI;
let transactionManager;

// Elementos do DOM
const elements = {
    accountNumber: document.getElementById('accountNumber'),
    changeAccountBtn: document.getElementById('changeAccountBtn'),
    accountType: document.getElementById('accountType'),
    startDate: document.getElementById('startDate'),
    endDate: document.getElementById('endDate'),
    filterBtn: document.getElementById('filterBtn'),
    downloadBtn: document.getElementById('downloadBtn'),
    emailBtn: document.getElementById('emailBtn'),
    notificationBtn: document.getElementById('notificationBtn'),
    notificationPanel: document.getElementById('notificationPanel'),
    transactionsBody: document.getElementById('transactionsBody'),
    loadingSpinner: document.getElementById('loadingSpinner'),
    errorMessage: document.getElementById('errorMessage'),
    emptyState: document.getElementById('emptyState'),
    transactionsTable: document.getElementById('transactionsTable'),
    sectionTitle: document.getElementById('sectionTitle'),
    currentBalance: document.getElementById('currentBalance'),
    totalIncome: document.getElementById('totalIncome'),
    totalExpenses: document.getElementById('totalExpenses'),
    emailModal: document.getElementById('emailModal'),
    emailForm: document.getElementById('emailForm'),
    cancelEmailBtn: document.getElementById('cancelEmailBtn')
};

// Inicialização da aplicação
async function initializeApp() {
    try {
        console.log('Inicializando aplicação...');

        // 1. Inicializar gerenciador de conta
        accountManager = new AccountManager();
        const hasAccount = accountManager.initialize();

        if (!hasAccount) {
            throw new Error('Número da conta não fornecido');
        }

        // Mostrar número da conta no header
        updateAccountDisplay();

        // 2. Inicializar gerenciador de transações
        transactionManager = new TransactionManager(CONFIG.API_BASE_URL, accountManager);
        window.transactionManager = transactionManager;

        // 3. Inicializar SSE Manager
        sseManager = new SSEManager(accountManager);

        // 4. Inicializar UI de Notificações
        notificationUI = new NotificationUI(sseManager);

        // 5. Conectar SSE
        sseManager.connect();

        // 6. Configurar listeners de eventos
        setupEventListeners();

        // 7. Inicializar datas e carregar dados iniciais
        initializeDates();
        await loadTransactions();

        console.log('Aplicação inicializada com sucesso!');

    } catch (error) {
        console.error('Erro ao inicializar aplicação:', error);
        showError('Erro ao inicializar aplicação. Por favor, recarregue a página.');
    }
}

// Atualizar exibição da conta
function updateAccountDisplay() {
    const numeroConta = accountManager.getNumeroConta();
    elements.accountNumber.textContent = `Conta: ${numeroConta}`;
}

// Configurar event listeners
function setupEventListeners() {
    // Trocar conta
    elements.changeAccountBtn.addEventListener('click', () => {
        if (confirm('Deseja trocar de conta? A conexão atual será encerrada.')) {
            sseManager.disconnect();
            if (accountManager.changeAccount()) {
                updateAccountDisplay();
                sseManager.reconnect();
                loadTransactions();
            }
        }
    });

    // Filtros
    elements.filterBtn.addEventListener('click', loadTransactions);
    elements.accountType.addEventListener('change', updateSectionTitle);

    // Ações
    elements.downloadBtn.addEventListener('click', handleDownload);
    elements.emailBtn.addEventListener('click', handleEmailClick);

    // Modal
    elements.emailForm.addEventListener('submit', handleEmailSubmit);
    elements.cancelEmailBtn.addEventListener('click', () => {
        elements.emailModal.classList.add('hidden');
    });

    elements.emailModal.querySelector('.close').addEventListener('click', () => {
        elements.emailModal.classList.add('hidden');
    });

    elements.emailModal.addEventListener('click', (e) => {
        if (e.target === elements.emailModal) {
            elements.emailModal.classList.add('hidden');
        }
    });

    // Listener customizado de notificações
    document.addEventListener('notificationClicked', (e) => {
        console.log('Notificação clicada:', e.detail);

        if (e.detail.type === NOTIFICATION_TYPES.TRANSACTION) {
            loadTransactions();
        }
    });
}

// Inicializar datas padrão (último mês)
function initializeDates() {
    const today = new Date();
    const lastMonth = new Date(today.getFullYear(), today.getMonth() - 1, today.getDate());

    elements.endDate.valueAsDate = today;
    elements.startDate.valueAsDate = lastMonth;
}

// Atualizar título da seção
function updateSectionTitle() {
    const type = elements.accountType.value;
    elements.sectionTitle.textContent = type === 'account'
        ? 'Extrato da Conta'
        : 'Fatura do Cartão de Crédito';
}

// Carregar transações
async function loadTransactions() {
    const type = elements.accountType.value;
    const startDate = elements.startDate.value;
    const endDate = elements.endDate.value;

    updateSectionTitle();

    elements.loadingSpinner.classList.remove('hidden');
    elements.errorMessage.classList.add('hidden');
    elements.emptyState.classList.add('hidden');
    elements.transactionsTable.classList.add('hidden');

    try {
        const result = await transactionManager.fetchTransactions(type, startDate, endDate);

        updateSummary(result.summary);

        if (result.transactions.length === 0) {
            elements.emptyState.classList.remove('hidden');
        } else {
            transactionManager.renderTransactions('transactionsBody');
            elements.transactionsTable.classList.remove('hidden');
        }
    } catch (error) {
        showError('Erro ao carregar transações. Por favor, tente novamente.');
    } finally {
        elements.loadingSpinner.classList.add('hidden');
    }
}

// Atualizar resumo
function updateSummary(summary) {
    elements.currentBalance.textContent = formatCurrency(summary.currentBalance);
    elements.totalIncome.textContent = formatCurrency(summary.totalIncome);
    elements.totalExpenses.textContent = formatCurrency(summary.totalExpenses);

    elements.currentBalance.className = 'amount';
    if (summary.currentBalance > 0) {
        elements.currentBalance.classList.add('positive');
    } else if (summary.currentBalance < 0) {
        elements.currentBalance.classList.add('negative');
    }
}

// Formatar moeda
function formatCurrency(value) {
    return new Intl.NumberFormat('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    }).format(value);
}

// Mostrar erro
function showError(message) {
    elements.errorMessage.textContent = message;
    elements.errorMessage.classList.remove('hidden');
}

// Download PDF
async function handleDownload() {
    const type = elements.accountType.value;
    const startDate = elements.startDate.value;
    const endDate = elements.endDate.value;

    elements.downloadBtn.disabled = true;
    elements.downloadBtn.textContent = '⏳ Gerando PDF...';

    try {
        await transactionManager.downloadPDF(type, startDate, endDate);
        showNotification('PDF baixado com sucesso!', 'success');
    } catch (error) {
        showError('Erro ao gerar PDF. Por favor, tente novamente.');
    } finally {
        elements.downloadBtn.disabled = false;
        elements.downloadBtn.textContent = '📥 Baixar PDF';
    }
}

// Enviar por e-mail
function handleEmailClick() {
    elements.emailModal.classList.remove('hidden');
    document.getElementById('emailAddress').value = '';
    document.getElementById('emailMessage').value = '';
}

async function handleEmailSubmit(e) {
    e.preventDefault();

    const email = document.getElementById('emailAddress').value;
    const message = document.getElementById('emailMessage').value;
    const type = elements.accountType.value;
    const startDate = elements.startDate.value;
    const endDate = elements.endDate.value;

    const submitBtn = elements.emailForm.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Enviando...';

    try {
        await transactionManager.sendEmail(email, message, type, startDate, endDate);
        elements.emailModal.classList.add('hidden');
        showNotification('E-mail enviado com sucesso!', 'success');
    } catch (error) {
        showError('Erro ao enviar e-mail. Por favor, tente novamente.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Enviar';
    }
}

// Notificações visuais (toast)
function showNotification(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 25px;
        background: ${type === 'success' ? '#10b981' : '#2563eb'};
        color: white;
        border-radius: 8px;
        box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1);
        z-index: 3000;
        animation: slideIn 0.3s ease;
    `;

    document.body.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Expor função globalmente
window.loadTransactions = loadTransactions;
window.showError = showError;

// Cleanup ao fechar página
window.addEventListener('beforeunload', () => {
    if (sseManager) {
        sseManager.disconnect();
    }
});

// Iniciar quando DOM estiver pronto
document.addEventListener('DOMContentLoaded', initializeApp);