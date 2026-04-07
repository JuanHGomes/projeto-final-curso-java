// Gerenciamento de UI de Notificações
class NotificationUI {
    constructor(sseManager) {
        this.sseManager = sseManager;
        this.notifications = [];
        this.unreadCount = 0;

        // Elementos DOM
        this.elements = {
            badge: document.getElementById('notificationBadge'),
            panel: document.getElementById('notificationPanel'),
            list: document.getElementById('notificationList'),
            btn: document.getElementById('notificationBtn'),
            connectionStatus: document.getElementById('connectionStatus')
        };

        this.init();
    }

    init() {
        // RF-FN05: Carregar histórico local
        this.loadStoredNotifications();

        // Configurar listeners SSE
        this.setupSSEListeners();

        // Configurar listeners UI
        this.setupUIListeners();

        // Atualizar UI inicial
        this.updateUI();
    }

    // RF-FN05: Carregar notificações do localStorage/sessionStorage
    loadStoredNotifications() {
        try {
            const stored = localStorage.getItem(CONFIG.NOTIFICATION_STORAGE_KEY);
            if (stored) {
                const data = JSON.parse(stored);
                this.notifications = data.notifications || [];
                this.calculateUnreadCount();
                console.log(`Carregadas ${this.notifications.length} notificações do armazenamento local`);
            }
        } catch (error) {
            console.error('Erro ao carregar notificações armazenadas:', error);
            this.notifications = [];
        }
    }

    // RF-FN05: Salvar notificações no localStorage
    saveNotifications() {
        try {
            // Limitar número de notificações armazenadas
            const toStore = this.notifications.slice(0, CONFIG.MAX_STORED_NOTIFICATIONS);

            localStorage.setItem(CONFIG.NOTIFICATION_STORAGE_KEY, JSON.stringify({
                notifications: toStore,
                lastUpdate: new Date().toISOString()
            }));
        } catch (error) {
            console.error('Erro ao salvar notificações:', error);
        }
    }

    // Configurar listeners do SSE
    setupSSEListeners() {
        // RF-FN02: Exibir notificações recebidas
        this.sseManager.addEventListener('notification', (data) => {
            this.addNotification(data);
        });

        // Listener de estado de conexão
        this.sseManager.addConnectionStateListener((newState, oldState) => {
            this.updateConnectionStatus(newState);
        });

        // Listener de máximo de tentativas
        this.sseManager.addEventListener('max_reconnect_attempts', () => {
            this.showReconnectPrompt();
        });
    }

    // Configurar listeners da UI
    setupUIListeners() {
        // Toggle painel
        this.elements.btn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.togglePanel();
        });

        // Fechar painel ao clicar fora
        document.addEventListener('click', (e) => {
            if (!this.elements.panel.contains(e.target) &&
                !this.elements.btn.contains(e.target)) {
                this.closePanel();
            }
        });

        // Delegação de eventos para notificações
        this.elements.list.addEventListener('click', (e) => {
            const notificationItem = e.target.closest('.notification-item');
            if (notificationItem) {
                const id = parseInt(notificationItem.dataset.id);
                this.handleNotificationClick(id);
            }

            // Botão de limpar todas
            if (e.target.closest('.clear-all-btn')) {
                this.clearAllNotifications();
            }

            // Botão de marcar todas como lidas
            if (e.target.closest('.mark-all-read-btn')) {
                this.markAllAsRead();
            }
        });
    }

    // RF-FN02: Adicionar nova notificação
    addNotification(data) {
        const notification = {
            id: Date.now() + Math.random(), // ID único
            eventId: data.eventId,
            type: data.type,
            title: this.getNotificationTitle(data),
            message: data.message || this.formatNotificationMessage(data),
            timestamp: data.timestamp || new Date().toISOString(),
            status: NOTIFICATION_STATUS.UNREAD,
            data: data
        };

        // Adicionar no início da lista
        this.notifications.unshift(notification);
        this.unreadCount++;

        // Salvar no armazenamento local
        this.saveNotifications();

        // Atualizar UI
        this.updateUI();

        // Mostrar toast
        this.showToast(notification);

        // Tocar som (opcional)
        this.playNotificationSound();

        console.log('Nova notificação adicionada:', notification);
    }

    // Obter título baseado no tipo
    getNotificationTitle(data) {
        switch (data.type) {
            case NOTIFICATION_TYPES.TRANSACTION:
                return '💳 Nova Transação';
            case NOTIFICATION_TYPES.ACCOUNT:
                return '🏦 Atualização de Conta';
            case NOTIFICATION_TYPES.CARD:
                return '💳 Cartão de Crédito';
            case NOTIFICATION_TYPES.ALERT:
                return '⚠️ Alerta';
            case NOTIFICATION_TYPES.SYSTEM:
                return '🔔 Sistema';
            default:
                return '📬 Notificação';
        }
    }

    // Formatar mensagem da notificação
    formatNotificationMessage(data) {
        if (data.type === NOTIFICATION_TYPES.TRANSACTION && data.transaction) {
            const t = data.transaction;
            const value = this.formatCurrency(t.value);
            const type = t.type === 'credit' ? 'crédito' : 'débito';
            return `Transação de ${type} de ${value} processada`;
        }
        return data.message || 'Nova notificação recebida';
    }

    // Atualizar UI completa
    updateUI() {
        this.updateBadge();
        this.renderNotifications();
    }

    // Atualizar badge de contador
    updateBadge() {
        if (this.unreadCount > 0) {
            this.elements.badge.textContent = this.unreadCount > 99 ? '99+' : this.unreadCount;
            this.elements.badge.classList.remove('hidden');
        } else {
            this.elements.badge.classList.add('hidden');
        }
    }

    // Renderizar lista de notificações
    renderNotifications() {
        if (this.notifications.length === 0) {
            this.elements.list.innerHTML = `
                <div class="empty-notifications">
                    <p>📭</p>
                    <p>Nenhuma notificação</p>
                </div>
            `;
            return;
        }

        const actionsHtml = `
            <div class="notification-actions">
                <button class="mark-all-read-btn">Marcar todas como lidas</button>
                <button class="clear-all-btn">Limpar todas</button>
            </div>
        `;

        const notificationsHtml = this.notifications
            .slice(0, 50) // Mostrar apenas as 50 mais recentes
            .map(notification => this.renderNotificationItem(notification))
            .join('');

        this.elements.list.innerHTML = actionsHtml + notificationsHtml;
    }

    // Renderizar item individual de notificação
    renderNotificationItem(notification) {
        const isUnread = notification.status === NOTIFICATION_STATUS.UNREAD;
        const typeClass = this.getTypeClass(notification.type);

        return `
            <div class="notification-item ${isUnread ? 'unread' : ''} ${typeClass}" 
                 data-id="${notification.id}">
                <div class="notification-header">
                    <span class="notification-title">${notification.title}</span>
                    ${isUnread ? '<span class="unread-dot"></span>' : ''}
                </div>
                <div class="notification-message">${this.escapeHtml(notification.message)}</div>
                <div class="notification-time">${this.formatTime(notification.timestamp)}</div>
            </div>
        `;
    }

    // Obter classe CSS baseada no tipo
    getTypeClass(type) {
        const typeMap = {
            [NOTIFICATION_TYPES.TRANSACTION]: 'type-transaction',
            [NOTIFICATION_TYPES.ACCOUNT]: 'type-account',
            [NOTIFICATION_TYPES.CARD]: 'type-card',
            [NOTIFICATION_TYPES.ALERT]: 'type-alert',
            [NOTIFICATION_TYPES.SYSTEM]: 'type-system'
        };
        return typeMap[type] || '';
    }

    // Handler de clique em notificação
    handleNotificationClick(id) {
        const notification = this.notifications.find(n => n.id === id);
        if (!notification) return;

        // Marcar como lida
        if (notification.status === NOTIFICATION_STATUS.UNREAD) {
            notification.status = NOTIFICATION_STATUS.READ;
            this.unreadCount = Math.max(0, this.unreadCount - 1);
            this.saveNotifications();
            this.updateUI();
        }

        // Ação específica baseada no tipo
        this.handleNotificationAction(notification);
    }

    // Ação específica da notificação
    handleNotificationAction(notification) {
        console.log('Notificação clicada:', notification);

        // Exemplo: se for transação, recarregar lista de transações
        if (notification.type === NOTIFICATION_TYPES.TRANSACTION) {
            if (window.transactionManager) {
                window.transactionManager.reload();
            }
        }

        // Emitir evento customizado
        document.dispatchEvent(new CustomEvent('notificationClicked', {
            detail: notification
        }));
    }

    // Marcar todas como lidas
    markAllAsRead() {
        this.notifications.forEach(n => {
            n.status = NOTIFICATION_STATUS.READ;
        });
        this.unreadCount = 0;
        this.saveNotifications();
        this.updateUI();
    }

    // Limpar todas as notificações
    clearAllNotifications() {
        if (confirm('Deseja realmente limpar todas as notificações?')) {
            this.notifications = [];
            this.unreadCount = 0;
            this.saveNotifications();
            this.updateUI();
        }
    }

    // Calcular contador de não lidas
    calculateUnreadCount() {
        this.unreadCount = this.notifications.filter(
            n => n.status === NOTIFICATION_STATUS.UNREAD
        ).length;
    }

    // Toggle painel
    togglePanel() {
        const isHidden = this.elements.panel.classList.contains('hidden');

        if (isHidden) {
            this.openPanel();
        } else {
            this.closePanel();
        }
    }

    // Abrir painel
    openPanel() {
        this.elements.panel.classList.remove('hidden');

        // Marcar como lidas após 2 segundos
        setTimeout(() => {
            this.markVisibleAsRead();
        }, 2000);
    }

    // Fechar painel
    closePanel() {
        this.elements.panel.classList.add('hidden');
    }

    // Marcar visíveis como lidas
    markVisibleAsRead() {
        let changed = false;

        this.notifications.slice(0, 10).forEach(notification => {
            if (notification.status === NOTIFICATION_STATUS.UNREAD) {
                notification.status = NOTIFICATION_STATUS.READ;
                this.unreadCount--;
                changed = true;
            }
        });

        if (changed) {
            this.unreadCount = Math.max(0, this.unreadCount);
            this.saveNotifications();
            this.updateUI();
        }
    }

    // Mostrar toast de notificação
    showToast(notification) {
        const toast = document.createElement('div');
        toast.className = 'notification-toast';
        toast.innerHTML = `
            <div class="toast-header">
                <strong>${notification.title}</strong>
                <button class="toast-close">&times;</button>
            </div>
            <div class="toast-body">${this.escapeHtml(notification.message)}</div>
        `;

        document.body.appendChild(toast);

        // Animar entrada
        setTimeout(() => toast.classList.add('show'), 10);

        // Remover ao clicar no X
        toast.querySelector('.toast-close').addEventListener('click', () => {
            this.removeToast(toast);
        });

        // Auto-remover após 5 segundos
        setTimeout(() => this.removeToast(toast), 5000);
    }

    // Remover toast
    removeToast(toast) {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }

    // Atualizar status de conexão
    updateConnectionStatus(state) {
        if (!this.elements.connectionStatus) return;

        const statusMap = {
            'disconnected': { text: 'Desconectado', class: 'status-disconnected' },
            'connecting': { text: 'Conectando...', class: 'status-connecting' },
            'connected': { text: 'Conectado', class: 'status-connected' },
            'reconnecting': { text: 'Reconectando...', class: 'status-reconnecting' },
            'error': { text: 'Erro de conexão', class: 'status-error' }
        };

        const status = statusMap[state] || statusMap['disconnected'];

        this.elements.connectionStatus.textContent = status.text;
        this.elements.connectionStatus.className = 'connection-status ' + status.class;
    }

    // Mostrar prompt de reconexão
    showReconnectPrompt() {
        const reconnect = confirm(
            'Não foi possível conectar ao servidor de notificações. Deseja tentar novamente?'
        );

        if (reconnect) {
            this.sseManager.reconnect();
        }
    }

    // Tocar som de notificação (opcional)
    playNotificationSound() {
        try {
            // Criar um beep simples usando Web Audio API
            const audioContext = new (window.AudioContext || window.webkitAudioContext)();
            const oscillator = audioContext.createOscillator();
            const gainNode = audioContext.createGain();

            oscillator.connect(gainNode);
            gainNode.connect(audioContext.destination);

            oscillator.frequency.value = 800;
            oscillator.type = 'sine';

            gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
            gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.5);

            oscillator.start(audioContext.currentTime);
            oscillator.stop(audioContext.currentTime + 0.5);
        } catch (error) {
            // Ignorar erros de áudio
        }
    }

    // Utilidades
    formatTime(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        const diff = now - date;

        const minutes = Math.floor(diff / 60000);
        const hours = Math.floor(diff / 3600000);
        const days = Math.floor(diff / 86400000);

        if (minutes < 1) return 'Agora';
        if (minutes < 60) return `${minutes}min atrás`;
        if (hours < 24) return `${hours}h atrás`;
        if (days < 7) return `${days}d atrás`;

        return date.toLocaleDateString('pt-BR', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    formatCurrency(value) {
        return new Intl.NumberFormat('pt-BR', {
            style: 'currency',
            currency: 'BRL'
        }).format(Math.abs(value));
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}