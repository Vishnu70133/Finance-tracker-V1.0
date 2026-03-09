package com.vishnu.finance_tracker.dto;
public class MonthlyChartDTO {

    private String month;
    private double income;
    private double expense;

    public MonthlyChartDTO(String month,double income,double expense){
        this.month = month;
        this.income = income;
        this.expense = expense;
    }

    public String getMonth(){ return month; }
    public double getIncome(){ return income; }
    public double getExpense(){ return expense; }
}