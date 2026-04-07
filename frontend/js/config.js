// Configurações centralizadas
const CONFIG = {
    API_BASE_URL: 'http://localhost:8080/api',
    SSE_NOTIFICATION_BASE_URL: 'http://localhost:8081/notificacao?numeroConta=',
    SSE_RECONNECT_INTERVAL: 3000,
    SSE_MAX_RECONNECT_ATTEMPTS: 10,
    NOTIFICATION_STORAGE_KEY: 'app_notifications',
    USER_ACCOUNT_KEY: 'user_account',
    MAX_STORED_NOTIFICATIONS: 100
};

// Tipos de transação (mapeamento do enum Java)
const TIPO_TRANSACAO = {
    DEBITO: 'DEBITO',
    CREDITO: 'CREDITO'
};

// Tipos de notificação
const NOTIFICATION_TYPES = {
    TRANSACTION: 'transaction',
    ACCOUNT: 'account',
    CARD: 'card',
    SYSTEM: 'system',
    ALERT: 'alert'
};

// Status de notificação
const NOTIFICATION_STATUS = {
    UNREAD: 'unread',
    READ: 'read'
};

// Mapeamento de status do histórico
const HISTORICO_STATUS = {
    'VALIDACAO_CONTA': 'Validação de Conta',
    'VALIDACAO_SALDO': 'Validação de Saldo',
    'VALIDACAO_CARTAO': 'Validação de Cartão',
    'PROCESSAMENTO': 'Processamento',
    'NOTIFICACAO_ENVIADA': 'Notificação Enviada',
    'CONCLUIDA': 'Concluída',
    'ERRO': 'Erro'
};