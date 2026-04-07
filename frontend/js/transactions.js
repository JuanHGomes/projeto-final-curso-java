class TransactionManager {
    constructor(apiBaseUrl, accountManager) {
        this.apiBaseUrl = apiBaseUrl;
        this.accountManager = accountManager;
        this.transactions = [];
    }

    async fetchTransactions(type, startDate, endDate) {
        const numeroConta = this.accountManager.getNumeroConta();

        const params = new URLSearchParams({
            type: type,
            startDate: startDate || '',
            endDate: endDate || ''
        });

        try {
            const response = await fetch(
                `${this.apiBaseUrl}/transacoes/${numeroConta}?${params}`
            );

            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }

            const data = await response.json();
            this.transactions = (data.transacoes || data || []).map(t => this.mapTransaction(t));

            return {
                transactions: this.transactions,
                summary: this.calculateSummary()
            };
        } catch (error) {
            console.error('Erro ao buscar transações:', error);
            throw error;
        }
    }

    mapTransaction(transacao) {
        return {
            id: `${transacao.numeroConta}-${transacao.timeStamp}`,
            numeroConta: transacao.numeroConta,
            date: transacao.timeStamp,
            description: transacao.estabelecimento || 'Transação',
            category: this.getCategoryFromEstabelecimento(transacao.estabelecimento),
            type: transacao.tipoTransacao === TIPO_TRANSACAO.CREDITO ? 'credit' : 'debit',
            value: transacao.valor / 100,
            valueInCents: transacao.valor,
            historico: transacao.historico,
            status: this.getTransactionStatus(transacao.historico),
            raw: transacao
        };
    }

    getCategoryFromEstabelecimento(estabelecimento) {
        if (!estabelecimento) return 'Outros';

        const categoryMap = {
            'mercado': 'Alimentação',
            'supermercado': 'Alimentação',
            'restaurante': 'Alimentação',
            'posto': 'Combustível',
            'farmacia': 'Saúde',
            'academia': 'Saúde',
            'shopping': 'Compras',
            'loja': 'Compras',
            'cinema': 'Lazer',
            'netflix': 'Lazer',
            'spotify': 'Lazer'
        };

        const estabelecimentoLower = estabelecimento.toLowerCase();

        for (const [key, category] of Object.entries(categoryMap)) {
            if (estabelecimentoLower.includes(key)) {
                return category;
            }
        }

        return 'Outros';
    }

    getTransactionStatus(historico) {
        if (!historico || Object.keys(historico).length === 0) {
            return 'Pendente';
        }

        if (historico['ERRO']) {
            return 'Erro';
        }

        if (historico['CONCLUIDA']) {
            return 'Concluída';
        }

        const steps = Object.entries(historico);
        const lastStep = steps[steps.length - 1];

        return HISTORICO_STATUS[lastStep[0]] || 'Em Processamento';
    }

    calculateSummary() {
        const summary = {
            currentBalance: 0,
            totalIncome: 0,
            totalExpenses: 0
        };

        this.transactions.forEach(transaction => {
            const value = transaction.value;

            if (transaction.type === 'credit') {
                summary.totalIncome += value;
                summary.currentBalance += value;
            } else {
                summary.totalExpenses += value;
                summary.currentBalance -= value;
            }
        });

        return summary;
    }

    renderTransactions(containerId) {
        const tbody = document.getElementById(containerId);

        if (this.transactions.length === 0) {
            tbody.innerHTML = '';
            return;
        }

        tbody.innerHTML = this.transactions.map(transaction => `
            <tr class="transaction-row" data-id="${transaction.id}">
                <td>${this.formatDate(transaction.date)}</td>
                <td>
                    <div class="transaction-description">
                        <strong>${transaction.description}</strong>
                        ${transaction.historico ? `
                            <button class="btn-details" onclick="showTransactionDetails('${transaction.id}')">
                                📋 Detalhes
                            </button>
                        ` : ''}
                    </div>
                </td>
                <td>${transaction.category}</td>
                <td>
                    <span class="transaction-type ${transaction.type}">
                        ${transaction.type === 'credit' ? 'Crédito' : 'Débito'}
                    </span>
                </td>
                <td>
                    <span class="transaction-value ${transaction.type === 'credit' ? 'positive' : 'negative'}">
                        ${transaction.type === 'credit' ? '+' : '-'} ${this.formatCurrency(transaction.value)}
                    </span>
                </td>
            </tr>
            ${transaction.historico ? this.renderHistoricoRow(transaction) : ''}
        `).join('');
    }

    renderHistoricoRow(transaction) {
        const historico = Object.entries(transaction.historico)
            .map(([step, success]) => `
                <div class="historico-step ${success ? 'success' : 'error'}">
                    <span class="step-icon">${success ? '✓' : '✗'}</span>
                    <span class="step-name">${HISTORICO_STATUS[step] || step}</span>
                </div>
            `)
            .join('');

        return `
            <tr class="historico-row hidden" id="historico-${transaction.id}">
                <td colspan="5">
                    <div class="historico-container">
                        <h4>Histórico de Processamento</h4>
                        <div class="historico-steps">
                            ${historico}
                        </div>
                    </div>
                </td>
            </tr>
        `;
    }

    formatDate(dateString) {
        const date = new Date(dateString);
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

    async downloadPDF(type, startDate, endDate) {
        const numeroConta = this.accountManager.getNumeroConta();

        const params = new URLSearchParams({
            type: type,
            startDate: startDate || '',
            endDate: endDate || '',
            format: 'pdf'
        });

        try {
            const response = await fetch(
                `${this.apiBaseUrl}/transacoes/${numeroConta}/download?${params}`
            );

            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `extrato_${numeroConta}_${new Date().getTime()}.pdf`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);

            return true;
        } catch (error) {
            console.error('Erro ao baixar PDF:', error);
            throw error;
        }
    }

    async sendEmail(email, message, type, startDate, endDate) {
        const numeroConta = this.accountManager.getNumeroConta();

        try {
            const response = await fetch(
                `${this.apiBaseUrl}/transacoes/${numeroConta}/email`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        email: email,
                        message: message,
                        type: type,
                        startDate: startDate,
                        endDate: endDate
                    })
                }
            );

            if (!response.ok) {
                throw new Error(`Erro HTTP: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Erro ao enviar e-mail:', error);
            throw error;
        }
    }

    async reload() {
        const type = document.getElementById('accountType').value;
        const startDate = document.getElementById('startDate').value;
        const endDate = document.getElementById('endDate').value;

        return await window.loadTransactions();
    }
}

window.showTransactionDetails = function(transactionId) {
    const historicoRow = document.getElementById(`historico-${transactionId}`);
    if (historicoRow) {
        historicoRow.classList.toggle('hidden');
    }
};