package com.tible.ocm.services.impl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tible.ocm.models.*;
import com.tible.ocm.models.mongo.Company;
import com.tible.ocm.models.mongo.LabelOrder;
import com.tible.ocm.models.mysql.ExistingBag;
import com.tible.ocm.models.mysql.ExistingTransaction;
import com.tible.ocm.models.mysql.QExistingBag;
import com.tible.ocm.models.mysql.QExistingTransaction;
import com.tible.ocm.services.CompanyService;
import com.tible.ocm.services.InformationLookupService;
import com.tible.ocm.services.LabelOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Slf4j
@Service
public class InformationLookupServiceImpl implements InformationLookupService {
    private static final String LABEL_USAGE_NUMBER_FIELD = "labelUsageNumber";
    private static final String RVM_OWNER_NUMBER_FIELD = "rvmOwnerNumber";
    // private static final String EXISTING_TRANSACTION_CONCAT_EXPRESSION = "concat(customerNumber,'0', number)";
    // private static final String EXISTING_BAG_CONCAT_EXPRESSION = "concat(customerNumber,'0', { $toString: \"$label\" })";
    private static final String CUSTOMER_NUMBER_FIELD = "customerNumber";
    private static final String CREATED_DATE_FIELD = "createdDate";
    private static final String NUMBER_DELIMITER = "0";

    private static final QExistingTransaction QET = QExistingTransaction.existingTransaction;
    private static final QExistingBag QEB = QExistingBag.existingBag;

    private final CompanyService companyService;
    private final LabelOrderService labelOrderService;
    private final JPAQueryFactory queryFactory;

    public InformationLookupServiceImpl(CompanyService companyService,
                                        LabelOrderService labelOrderService,
                                        JPAQueryFactory queryFactory) {
        this.companyService = companyService;
        this.labelOrderService = labelOrderService;
        this.queryFactory = queryFactory;
    }

    @Override
    public LabelUsageResponse getLabelUsage(String rvmOwnerNumber, String labelUsageNumber) {
        return Optional.ofNullable(findTransaction(rvmOwnerNumber, labelUsageNumber))
                .map(transaction -> buildLabelUsageResponse(labelUsageNumber, transaction))
                .orElseGet(() ->
                        Optional.ofNullable(findBag(rvmOwnerNumber, labelUsageNumber))
                            .map(bag -> buildLabelUsageResponse(labelUsageNumber, bag))
                            .orElse(null));
    }

    @Override
    public LabelIssuedResponse getLabelIssued(String rvmOwnerNumber, String localizationNumber) {
        List<LabelOrder> labelOrders = labelOrderService.findAllByRvmOwnerNumberAndCustomerLocalizationNumber(rvmOwnerNumber, localizationNumber);

        List<LabelOrderResponse> issuedLabelResponses = labelOrders
                .stream()
                .map(this::buildLabelOrderResponse)
                .collect(toList());

        return buildLabelIssuedResponse(localizationNumber, issuedLabelResponses);
    }

    @Override
    public GlnUsageResponse getGlnUsage(String rvmOwnerNumber, String localizationNumber, int daysInPast) {
        Map<LocalDate, List<String>> bags = getBagsUsedLabels(rvmOwnerNumber, localizationNumber, daysInPast);
        Map<LocalDate, List<String>> transactions = getTransactionsUsedLabels(rvmOwnerNumber, localizationNumber, daysInPast);

        Map<LocalDate, List<String>> usedLabelsMap = new HashMap<>(bags);
        transactions.forEach(mergeMap(usedLabelsMap));

        List<LabelResponse> labelResponses = usedLabelsMap
                .entrySet()
                .stream()
                .map(entry -> buildLabelResponse(entry.getKey(), entry.getValue()))
                .collect(toList());

        return buildGlnUsageResponse(localizationNumber, labelResponses);
    }

    @Override
    public CustomerNumbersResponse getCustomerNumbers(String rvmOwnerNumber, String localizationNumber) {
        List<String> customerNumbers = getCompaniesByLocalizationNumber(rvmOwnerNumber, localizationNumber)
                .stream()
                .map(Company::getNumber)
                .collect(Collectors.toList());

        return buildCustomerNumberResponse(localizationNumber, customerNumbers);
    }

    private BiConsumer<LocalDate, List<String>> mergeMap(Map<LocalDate, List<String>> usedLabelsMap) {
        return (key, value) ->
                usedLabelsMap.merge(key, value, (v1, v2) -> {
                    v1.addAll(v2);
                    return v1;
                });
    }

    private ExistingTransaction findTransaction(String rvmOwnerNumber, String labelUsageNumber) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QET.rvmOwnerNumber.eq(rvmOwnerNumber));
        builder.and(QET.combinedCustomerNumberLabel.eq(labelUsageNumber).or(QET.number.eq(labelUsageNumber)));
        return queryFactory.selectFrom(QET).where(builder).fetchFirst();
    }

    private ExistingBag findBag(String rvmOwnerNumber, String labelUsageNumber) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QEB.rvmOwnerNumber.eq(rvmOwnerNumber));
        builder.and(QEB.combinedCustomerNumberLabel.eq(labelUsageNumber));
        return queryFactory.selectFrom(QEB).where(builder).fetchFirst();
    }

    private LabelUsageResponse buildLabelUsageResponse(String labelUsageNumber, ExistingTransaction transaction) {
        return LabelUsageResponse.builder()
                .number(labelUsageNumber)
                .localizationNumber(getCompanyLocalizationNumber(transaction.getCustomerNumber()))
                .transactionCombinedNumber(transaction.getTransactionCombinedNumber())
                .build();
    }

    private LabelUsageResponse buildLabelUsageResponse(String labelUsageNumber, ExistingBag bag) {
        return LabelUsageResponse.builder()
                .number(labelUsageNumber)
                .localizationNumber(getCompanyLocalizationNumber(bag.getCustomerNumber()))
                .build();
    }

    private CustomerNumbersResponse buildCustomerNumberResponse(String localizationNumber, List<String> customerNumbers) {
        return CustomerNumbersResponse.builder()
                .localizationNumber(localizationNumber)
                .customerNumbers(customerNumbers)
                .build();
    }

    private String getCompanyLocalizationNumber(String companyNumber) {
        Company company = companyService.findByNumber(companyNumber);
        return company != null ? company.getLocalizationNumber() : null;
    }

    private Map<LocalDate, List<String>> getBagsUsedLabels(String rvmOwnerNumber, String localizationNumber, int daysInPast) {
        return findBags(rvmOwnerNumber, localizationNumber, daysInPast)
                .stream()
                .collect(groupingBy(ExistingBag::getCreatedDate, mapping(ExistingBag::getCombinedCustomerNumberLabel, toList())));
    }

    private Map<LocalDate, List<String>> getTransactionsUsedLabels(String rvmOwnerNumber, String localizationNumber, int daysInPast) {
        return findTransactions(rvmOwnerNumber, localizationNumber, daysInPast)
                .stream()
                .collect(groupingBy(ExistingTransaction::getCreatedDate, mapping(ExistingTransaction::getCombinedCustomerNumberLabel, toList())));
    }

    private List<String> getRelatedCustomerNumbersByLocalizationNumber(String rvmOwnerNumber, String localizationNumber) {
        return getCompaniesByLocalizationNumber(rvmOwnerNumber, localizationNumber)
                .stream()
                .map(Company::getNumber)
                .collect(Collectors.toList());
    }

    private List<ExistingBag> findBags(String rvmOwnerNumber, String localizationNumber, int daysInPast) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QEB.rvmOwnerNumber.eq(rvmOwnerNumber));
        builder.and(QEB.customerNumber.in(getRelatedCustomerNumbersByLocalizationNumber(rvmOwnerNumber, localizationNumber)));
        builder.and(QEB.createdDate.goe(LocalDate.now().minusDays(daysInPast)));
        return queryFactory.selectFrom(QEB).where(builder).fetch();
    }

    private List<ExistingTransaction> findTransactions(String rvmOwnerNumber, String localizationNumber, int daysInPast) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(QET.rvmOwnerNumber.eq(rvmOwnerNumber));
        builder.and(QET.customerNumber.in(getRelatedCustomerNumbersByLocalizationNumber(rvmOwnerNumber, localizationNumber)));
        builder.and(QET.createdDate.goe(LocalDate.now().minusDays(daysInPast)));
        return queryFactory.selectFrom(QET).where(builder).fetch();
    }

    /*@Deprecated
    private <T> List<T> findBagsOrTransactions(String rvmOwnerNumber, String localizationNumber, int daysInPast,
                                               Class<T> tClass) {
        List<String> relatedCustomerNumbers = getCompaniesByLocalizationNumber(localizationNumber, rvmOwnerNumber)
                .stream()
                .map(Company::getNumber)
                .collect(Collectors.toList());

        Criteria customerNumbersCriteria = Criteria.where(CUSTOMER_NUMBER_FIELD).in(relatedCustomerNumbers);
        Criteria rvmOwnerNumberCriteria = Criteria.where(RVM_OWNER_NUMBER_FIELD).is(rvmOwnerNumber);
        Criteria createdDateCriteria = Criteria.where(CREATED_DATE_FIELD).gte(LocalDate.now().minusDays(daysInPast));
        MatchOperation customerNumbersMatch = Aggregation.match(customerNumbersCriteria);
        MatchOperation rvmOwnerNumberMatch = Aggregation.match(rvmOwnerNumberCriteria);
        MatchOperation createdDateMatch = Aggregation.match(createdDateCriteria);
        Aggregation aggregation = Aggregation.newAggregation(rvmOwnerNumberMatch, customerNumbersMatch, createdDateMatch);

        return mongoTemplate
                .aggregate(aggregation, tClass, tClass)
                .getMappedResults();
    }*/

    /*private String buildNumber(ExistingBag bag) {
        return bag.getCustomerNumber() + NUMBER_DELIMITER + bag.getLabel();
    }*/

    /*private String buildNumber(ExistingTransaction transaction) {
        return transaction.getCustomerNumber() + NUMBER_DELIMITER + transaction.getNumber();
    }*/

    private List<Company> getCompaniesByLocalizationNumber(String rvmOwnerNumber, String localizationNumber) {
        return companyService.findByLocalizationNumberAndRvmOwnerNumber(localizationNumber, rvmOwnerNumber);
    }

    private LabelResponse buildLabelResponse(LocalDate createdDate, List<String> numbers) {
        return LabelResponse.builder()
                .date(createdDate)
                .numbers(numbers)
                .build();
    }

    private GlnUsageResponse buildGlnUsageResponse(String localizationNumber, List<LabelResponse> usedLabels) {
        return GlnUsageResponse.builder()
                .localizationNumber(localizationNumber)
                .usedLabels(usedLabels)
                .build();
    }

    private LabelOrderResponse buildLabelOrderResponse(LabelOrder labelOrder) {
        return LabelOrderResponse.builder()
                .date(labelOrder.getOrderDate().toLocalDate())
                .firstLabel(labelOrder.getFirstLabelNumber())
                .lastLabel(labelOrder.getLastLabelNumber())
                .quantity(labelOrder.getQuantity())
                .balance(labelOrder.getBalance())
                .build();
    }

    private LabelIssuedResponse buildLabelIssuedResponse(String localizationNumber, List<LabelOrderResponse> labelOrders) {
        return LabelIssuedResponse.builder()
                .localizationNumber(localizationNumber)
                .issuedLabels(labelOrders)
                .build();
    }
}
