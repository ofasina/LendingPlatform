/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fash.LendingPlatform.service;

import com.fash.LendingPlatform.model.LoanCustomer;
import com.fash.LendingPlatform.model.LoanProduct;
import com.fash.LendingPlatform.payload.LendingRequestPayload;
import com.fash.LendingPlatform.payload.LendingResponsePayload;
import com.fash.LendingPlatform.payload.LoanOfferPayload;
import com.fash.LendingPlatform.repository.LendingPlatformRepository;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 *
 * @author ofasina
 */
@Service
public class LendingPlatformServiceImpl implements LendingPlatformService {

    private static final Logger LOGGER = Logger.getLogger(LendingPlatformService.class.getName());
    private static String AUTHORIZATION = "";

    @Autowired
    LendingPlatformRepository lendingPlatformRepository;
    @Autowired
    MessageSource messageSource;
    @Autowired
    Environment env;
    @Autowired
    Gson gson;

    @Override
    public boolean checkRequestHeaderValidity(String authorization) {
        //Check if the request header is valid
        AUTHORIZATION = "Basic " + Base64.getEncoder().encodeToString((env.getProperty("api.lending.authorization.username") + ":" + env.getProperty("api.lending.authorization.password").trim()).getBytes());
        return AUTHORIZATION.equals(authorization);
    }

    @Override
    public String loanOffer(LendingRequestPayload requestPayload) {
        try {
            LendingResponsePayload responsePayload = new LendingResponsePayload();
            //get the customer loan maximum qualification
            LoanCustomer qualifiedCustomer = lendingPlatformRepository.getCustomerQualification(requestPayload.getMobileNumber().trim());
            if (qualifiedCustomer == null) {
                responsePayload.setResponseCode(env.getProperty("api.lending.record.not.found.code"));
                responsePayload.setResponseMessage(messageSource.getMessage("appMessages.customer.profile.missing", new Object[]{requestPayload.getMobileNumber().trim()}, Locale.ENGLISH));
                String responseJson = gson.toJson(responsePayload);
                return responseJson;
            }
            //get the product customer is qualified for
            List<LoanProduct> products = lendingPlatformRepository.getLoanList();
            List<LoanOfferPayload> offers = new ArrayList<>();
            for (LoanProduct qualifiedAmount : products) {
                LoanOfferPayload loanProduct = new LoanOfferPayload();
                double qualifiedCustomerAmount = Double.valueOf(qualifiedCustomer.getQualifiedLoanAmount());
                double productAmount = Double.valueOf(qualifiedAmount.getMaxLimit());
                if (qualifiedCustomerAmount >= productAmount) {
                    loanProduct.setLoanType(qualifiedAmount.getProductType());
                    loanProduct.setLoanAmount(qualifiedAmount.getMaxLimit());
                    loanProduct.setInterestRate(qualifiedAmount.getInterestPercentage());
                    loanProduct.setTenure(qualifiedAmount.getTenure());                 
                }
                offers.add(loanProduct);
            }
            responsePayload.setResponseCode(env.getProperty("api.lending.success.code"));
            responsePayload.setLoanOffers(offers);
            String responseJson = gson.toJson(responsePayload);

            return responseJson;
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public String loanOfferApplication(LendingRequestPayload requestPayload) {
        double loanAmount = Double.valueOf(requestPayload.getLoanAmount());
        double minimumAmount = Double.valueOf(env.getProperty("minimum.loan.amount"));
        LendingResponsePayload responsePayload = new LendingResponsePayload();
        if (loanAmount <= minimumAmount) {
            responsePayload.setResponseCode(env.getProperty("api.lending.failed.code"));
            responsePayload.setResponseMessage("Sorry, loan application failed.");

            String responseJson = gson.toJson(responsePayload);
            return responseJson;
        }
        responsePayload.setResponseCode(env.getProperty("api.lending.success.code"));
        responsePayload.setResponseMessage("Loan application successful.");
        String responseJson = gson.toJson(responsePayload);
        return responseJson;
    }

}
