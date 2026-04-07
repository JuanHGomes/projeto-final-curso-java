// Gerenciamento de Server-Sent Events (SSE) - Versão Simplificada
class SSEManager {
    constructor(accountManager) {
        this.accountManager = accountManager;
        this.eventSource = null;
        this.reconnectAttempts = 0;
        this.reconnectTimer = null;
        this.isConnecting = false;
        this.isIntentionallyClosed = false;
        this.listeners = new Map();
        this.connectionStateListeners = [];
        this.lastEventId = null;

        this.connectionStates = {
            DISCONNECTED: 'disconnected',
            CONNECTING: 'connecting',
            CONNECTED: 'connected',
            RECONNECTING: 'reconnecting',
            ERROR: 'error'
        };

        this.currentState = this.connectionStates.DISCONNECTED;
    }

    // RF-FN01 e RF-FN03: Conectar SSE com número da conta no path
    connect() {
        if (this.isConnecting || this.currentState === this.connectionStates.CONNECTED) {
            console.log('SSE: Já conectado ou conectando');
            return;
        }

        if (!this.accountManager.hasAccount()) {
            console.error('SSE: Número da conta não definido');
            this.updateConnectionState(this.connectionStates.ERROR);
            return;
        }

        this.isConnecting = true;
        this.isIntentionallyClosed = false;
        this.updateConnectionState(this.connectionStates.CONNECTING);

        try {
            const numeroConta = this.accountManager.getNumeroConta();

            // URL: http://localhost:8080/api/notifications/stream/{numeroConta}
            const url = `${CONFIG.SSE_NOTIFICATION_BASE_URL}${numeroConta}`;

            console.log('SSE: Conectando para conta:', numeroConta);
            console.log('SSE: URL:', url);

            this.eventSource = new EventSource(url);

            // Handler: Conexão aberta
            this.eventSource.onopen = () => {
                console.log('SSE: Conexão estabelecida com sucesso');
                this.isConnecting = false;
                this.reconnectAttempts = 0;
                this.updateConnectionState(this.connectionStates.CONNECTED);

                if (this.reconnectTimer) {
                    clearTimeout(this.reconnectTimer);
                    this.reconnectTimer = null;
                }
            };

            // Handler: Mensagem genérica
            this.eventSource.onmessage = (event) => {
                this.handleMessage(event);
            };

            // Handler: Erro
            this.eventSource.onerror = (error) => {
                console.error('SSE: Erro na conexão', error);
                this.isConnecting = false;

                // RF-FN04: Reconectar automaticamente
                if (!this.isIntentionallyClosed) {
                    this.handleConnectionError();
                }
            };

            // Configurar listeners para eventos específicos
            this.setupEventListeners();

        } catch (error) {
            console.error('SSE: Erro ao criar conexão:', error);
            this.isConnecting = false;
            this.updateConnectionState(this.connectionStates.ERROR);
            this.scheduleReconnect();
        }
    }

    // Configurar listeners para eventos do backend
    setupEventListeners() {
        // Evento: notificacao
        // Backend envia: event: notificacao
        // data: {"numeroConta":"12345678","mensagem":"Mensagem aqui"}
        this.eventSource.addEventListener('notificacao', (event) => {
            this.handleNotificacaoEvent(event);
        });

        // Evento: transacao
        // Backend envia: event: transacao
        // data: {objeto Transacao completo}
        this.eventSource.addEventListener('transacao', (event) => {
            this.handleTransacaoEvent(event);
        });

        // Evento: heartbeat (opcional)
        this.eventSource.addEventListener('heartbeat', (event) => {
            console.log('SSE: Heartbeat recebido');
            this.lastEventId = event.lastEventId;
        });

        // Evento: connected (quando backend confirma conexão)
        this.eventSource.addEventListener('connected', (event) => {
            console.log('SSE: Confirmação de conexão recebida');
            const data = JSON.parse(event.data);
            console.log('SSE: Conectado à conta:', data.numeroConta);
        });
    }

    // Processar mensagem genérica
    handleMessage(event) {
        try {
            this.lastEventId = event.lastEventId;
            const data = JSON.parse(event.data);
            console.log('SSE: Mensagem recebida:', data);
            this.notifyListeners('message', data);
        } catch (error) {
            console.error('SSE: Erro ao processar mensagem:', error);
        }
    }

    // Handler para evento de notificação
    handleNotificacaoEvent(event) {
        try {
            this.lastEventId = event.lastEventId;

            // Parsear: { numeroConta: string, mensagem: string }
            const notificacao = JSON.parse(event.data);

            console.log('SSE: Notificação recebida:', notificacao);

            const notification = {
                type: NOTIFICATION_TYPES.SYSTEM,
                eventId: event.lastEventId,
                timestamp: new Date().toISOString(),
                numeroConta: notificacao.numeroConta,
                message: notificacao.mensagem,
                data: notificacao
            };

            this.notifyListeners('notificacao', notification);
            this.notifyListeners('notification', notification);
        } catch (error) {
            console.error('SSE: Erro ao processar notificação:', error);
        }
    }

    // Handler para evento de transação
    handleTransacaoEvent(event) {
        try {
            this.lastEventId = event.lastEventId;

            // Parsear objeto Transacao completo
            const transacao = JSON.parse(event.data);

            console.log('SSE: Transação recebida:', transacao);

            const notification = {
                type: NOTIFICATION_TYPES.TRANSACTION,
                eventId: event.lastEventId,
                timestamp: transacao.timeStamp,
                numeroConta: transacao.numeroConta,
                message: this.formatTransactionMessage(transacao),
                transaction: transacao,
                data: transacao
            };

            this.notifyListeners('transacao', notification);
            this.notifyListeners('notification', notification);
        } catch (error) {
            console.error('SSE: Erro ao processar transação:', error);
        }
    }

    // Formatar mensagem de transação
    formatTransactionMessage(transacao) {
        const tipo = transacao.tipoTransacao === TIPO_TRANSACAO.CREDITO ? 'crédito' : 'débito';
        const valor = this.formatCurrency(transacao.valor);
        const estabelecimento = transacao.estabelecimento || 'Estabelecimento';

        return `Transação de ${tipo} de ${valor} em ${estabelecimento}`;
    }

    formatCurrency(value) {
        return new Intl.NumberFormat('pt-BR', {
            style: 'currency',
            currency: 'BRL'
        }).format(value / 100);
    }

    // RF-FN04: Reconexão automática
    handleConnectionError() {
        this.updateConnectionState(this.connectionStates.RECONNECTING);

        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }

        this.scheduleReconnect();
    }

    scheduleReconnect() {
        if (this.isIntentionallyClosed) {
            return;
        }

        if (this.reconnectAttempts >= CONFIG.SSE_MAX_RECONNECT_ATTEMPTS) {
            console.error('SSE: Número máximo de tentativas de reconexão atingido');
            this.updateConnectionState(this.connectionStates.ERROR);
            this.notifyListeners('max_reconnect_attempts', {
                attempts: this.reconnectAttempts
            });
            return;
        }

        this.reconnectAttempts++;

        const delay = Math.min(
            CONFIG.SSE_RECONNECT_INTERVAL * Math.pow(2, this.reconnectAttempts - 1),
            60000
        );

        console.log(`SSE: Tentando reconectar em ${delay}ms (tentativa ${this.reconnectAttempts}/${CONFIG.SSE_MAX_RECONNECT_ATTEMPTS})`);

        this.reconnectTimer = setTimeout(() => {
            console.log('SSE: Iniciando reconexão...');
            this.connect();
        }, delay);
    }

    addEventListener(type, callback) {
        if (!this.listeners.has(type)) {
            this.listeners.set(type, []);
        }
        this.listeners.get(type).push(callback);
    }

    removeEventListener(type, callback) {
        if (this.listeners.has(type)) {
            const callbacks = this.listeners.get(type);
            const index = callbacks.indexOf(callback);
            if (index > -1) {
                callbacks.splice(index, 1);
            }
        }
    }

    notifyListeners(type, data) {
        if (this.listeners.has(type)) {
            this.listeners.get(type).forEach(callback => {
                try {
                    callback(data);
                } catch (error) {
                    console.error(`Erro ao executar listener ${type}:`, error);
                }
            });
        }
    }

    addConnectionStateListener(callback) {
        this.connectionStateListeners.push(callback);
    }

    updateConnectionState(newState) {
        const oldState = this.currentState;
        this.currentState = newState;

        console.log(`SSE: Estado alterado de ${oldState} para ${newState}`);

        this.connectionStateListeners.forEach(callback => {
            try {
                callback(newState, oldState);
            } catch (error) {
                console.error('Erro ao executar listener de estado:', error);
            }
        });
    }

    getConnectionState() {
        return this.currentState;
    }

    isConnected() {
        return this.currentState === this.connectionStates.CONNECTED;
    }

    disconnect() {
        console.log('SSE: Desconectando...');
        this.isIntentionallyClosed = true;

        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }

        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }

        this.updateConnectionState(this.connectionStates.DISCONNECTED);
    }

    reconnect() {
        console.log('SSE: Reconexão manual solicitada');
        this.disconnect();
        this.reconnectAttempts = 0;
        setTimeout(() => this.connect(), 100);
    }
}