package br.com.deivid.finance.fatura;

public enum StatusFatura {
    ABERTA,   // ainda recebendo compras
    FECHADA,  // fechou, esperando pagamento
    PAGA      // paga
}
