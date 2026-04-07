// Gerenciamento simples do número da conta
class AccountManager {
    constructor() {
        this.numeroConta = null;
    }

    // Inicializar com número da conta
    initialize() {
        // Tentar recuperar do localStorage
        this.numeroConta = localStorage.getItem(CONFIG.USER_ACCOUNT_KEY);

        if (this.numeroConta) {
            console.log('Conta recuperada do localStorage:', this.numeroConta);
            return true;
        }

        // Se não houver, solicitar ao usuário
        return this.promptForAccount();
    }

    // Solicitar número da conta ao usuário
    promptForAccount() {
        const conta = prompt('Digite o número da sua conta:');

        if (conta && conta.trim()) {
            this.setNumeroConta(conta.trim());
            return true;
        }

        alert('Número da conta é obrigatório!');
        return false;
    }

    // Definir número da conta
    setNumeroConta(numeroConta) {
        this.numeroConta = numeroConta;
        localStorage.setItem(CONFIG.USER_ACCOUNT_KEY, numeroConta);
        console.log('Conta definida:', numeroConta);
    }

    // Obter número da conta
    getNumeroConta() {
        return this.numeroConta;
    }

    // Verificar se tem conta definida
    hasAccount() {
        return !!this.numeroConta;
    }

    // Limpar conta
    clearAccount() {
        this.numeroConta = null;
        localStorage.removeItem(CONFIG.USER_ACCOUNT_KEY);
    }

    // Trocar de conta
    changeAccount() {
        this.clearAccount();
        return this.promptForAccount();
    }
}