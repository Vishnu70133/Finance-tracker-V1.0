package com.vishnu.finance_tracker.model;

import com.vishnu.finance_tracker.dto.FinanceQueryDTO;

public class PendingAction {

    private String action; // ADD_TRANSACTION, UPDATE_TRANSACTION
    private FinanceQueryDTO query;

    public PendingAction(String action, FinanceQueryDTO query) {
        this.action = action;
        this.query = query;
    }

    public String getAction() {
        return action;
    }

    public FinanceQueryDTO getQuery() {
        return query;
    }
}