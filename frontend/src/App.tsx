const { useState, useEffect, useMemo } = React;

interface Transacao {
    numeroConta: string;
    valor: number;
    tipoTransacao: 'DEBITO' | 'CREDITO';
    timeStamp: string;
    estabelecimento: string;
}

interface Notificacao {
    numeroConta: string;
    mensagem: string;
}

const App = () => {
    const [numeroConta, setNumeroConta] = useState('123');
    const [extrato, setExtrato] = useState<Transacao[]>([]);
    const [notificacoes, setNotificacoes] = useState<string[]>([]);
    const [loading, setLoading] = useState(false);
    const [transacaoData, setTransacaoData] = useState({
        valor: 100,
        tipoTransacao: 'DEBITO',
        estabelecimento: 'Lojinha do Bairro'
    });

    const fetchExtrato = async (conta: string) => {
        setLoading(true);
        try {
            const response = await fetch(`http://localhost:8084/registro-transacao/extrato/${conta}`);
            if (response.ok) {
                const data = await response.json();
                setExtrato(data);
            }
        } catch (error) {
            console.error("Erro ao buscar extrato:", error);
        } finally {
            setLoading(false);
        }
    };

    const executarTransacao = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await fetch(`http://localhost:8082/transacao/executarTransacao`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    numeroConta,
                    valor: transacaoData.valor,
                    tipoTransacao: transacaoData.tipoTransacao,
                    estabelecimento: transacaoData.estabelecimento
                })
            });
            if (response.ok) {
                console.log("Transação enviada!");
                // Notification will update the state via SSE
            }
        } catch (error) {
            console.error("Erro ao executar transação:", error);
        }
    };

    useEffect(() => {
        if (numeroConta) {
            fetchExtrato(numeroConta);
            
            // SSE Connection
            const eventSource = new EventSource(`http://localhost:8081/notificacao?numeroConta=${numeroConta}`);
            
            eventSource.onmessage = (event) => {
                const data: Notificacao = JSON.parse(event.data);
                setNotificacoes(prev => [...prev, data.mensagem]);
                // Auto-refresh extrato after a new notification
                fetchExtrato(numeroConta);
                
                // Remove notification after 5 seconds
                setTimeout(() => {
                    setNotificacoes(prev => prev.filter(n => n !== data.mensagem));
                }, 5000);
            };

            eventSource.onerror = (err) => {
                console.error("SSE Error:", err);
                eventSource.close();
            };

            return () => {
                eventSource.close();
            };
        }
    }, [numeroConta]);

    return (
        <div className="container mx-auto p-4 max-w-4xl">
            <header className="mb-8 text-center">
                <h1 className="text-3xl font-bold text-gray-800">Bank Simulation Dashboard</h1>
                <p className="text-gray-600">Real-time Transaction Monitoring</p>
            </header>

            {notificacoes.map((msg, idx) => (
                <div key={idx} className="notification-badge bg-blue-600 shadow-lg border border-blue-400">
                    <p className="font-bold">Nova Notificação!</p>
                    <p>{msg}</p>
                </div>
            ))}

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="md:col-span-1">
                    <section className="card">
                        <h2 className="text-xl font-semibold mb-4">Configurações</h2>
                        <label className="block text-sm font-medium text-gray-700">Número da Conta</label>
                        <input 
                            type="text" 
                            className="input-field" 
                            value={numeroConta} 
                            onChange={(e) => setNumeroConta(e.target.value)}
                        />
                        <button 
                            className="btn btn-primary w-full mt-4"
                            onClick={() => fetchExtrato(numeroConta)}
                        >
                            Atualizar Extrato
                        </button>
                    </section>

                    <section className="card">
                        <h2 className="text-xl font-semibold mb-4">Simular Transação</h2>
                        <form onSubmit={executarTransacao}>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700">Valor (centavos)</label>
                                <input 
                                    type="number" 
                                    className="input-field" 
                                    value={transacaoData.valor} 
                                    onChange={(e) => setTransacaoData({...transacaoData, valor: parseInt(e.target.value)})}
                                />
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700">Tipo</label>
                                <select 
                                    className="input-field" 
                                    value={transacaoData.tipoTransacao}
                                    onChange={(e) => setTransacaoData({...transacaoData, tipoTransacao: e.target.value})}
                                >
                                    <option value="DEBITO">Débito</option>
                                    <option value="CREDITO">Crédito</option>
                                </select>
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700">Estabelecimento</label>
                                <input 
                                    type="text" 
                                    className="input-field" 
                                    value={transacaoData.estabelecimento} 
                                    onChange={(e) => setTransacaoData({...transacaoData, estabelecimento: e.target.value})}
                                />
                            </div>
                            <button type="submit" className="btn bg-green-600 text-white w-full hover:bg-green-700">
                                Executar
                            </button>
                        </form>
                    </section>
                </div>

                <div className="md:col-span-2">
                    <section className="card">
                        <h2 className="text-xl font-semibold mb-4">Extrato Bancário</h2>
                        {loading ? (
                            <p className="text-center py-4">Carregando...</p>
                        ) : extrato.length === 0 ? (
                            <p className="text-center py-4 text-gray-500">Nenhuma transação encontrada.</p>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="min-w-full divide-y divide-gray-200">
                                    <thead>
                                        <tr>
                                            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Data</th>
                                            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Estabelecimento</th>
                                            <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tipo</th>
                                            <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Valor</th>
                                        </tr>
                                    </thead>
                                    <tbody className="bg-white divide-y divide-gray-200">
                                        {extrato.map((t, i) => (
                                            <tr key={i}>
                                                <td className="px-4 py-2 whitespace-nowrap text-sm text-gray-500">
                                                    {new Date(t.timeStamp).toLocaleString()}
                                                </td>
                                                <td className="px-4 py-2 whitespace-nowrap text-sm text-gray-900 font-medium">
                                                    {t.estabelecimento}
                                                </td>
                                                <td className="px-4 py-2 whitespace-nowrap text-sm">
                                                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${t.tipoTransacao === 'CREDITO' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                                                        {t.tipoTransacao}
                                                    </span>
                                                </td>
                                                <td className={`px-4 py-2 whitespace-nowrap text-sm text-right font-mono ${t.tipoTransacao === 'CREDITO' ? 'text-green-600' : 'text-red-600'}`}>
                                                    {t.tipoTransacao === 'CREDITO' ? '+' : '-'} R$ {(t.valor / 100).toFixed(2)}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </section>
                </div>
            </div>
        </div>
    );
};

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);
