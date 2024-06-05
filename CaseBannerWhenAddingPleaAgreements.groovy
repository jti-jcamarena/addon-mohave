import com.sustain.document.model.Document;
import com.sustain.cases.model.Case;
import com.sustain.DomainObject;
import com.sustain.expression.Where;
import com.sustain.cases.model.Party;
import com.sustain.cases.model.CaseSpecialStatus;
import com.sustain.util.DateUtil;
import com.sustain.condition.model.Condition;

def Document pleaAgreementDocument = _document;
def String caseSpecialStatusPleaStatus = "PLEADEFCASES";
def Date caseSpecialStatusDate = DateUtil.today;
def String caseSpecialStatusValue = "Please verify plea includes ALL the Defendant's cases. YOU MUST review Defendant case involvements before proceeding.";
def String partyActiveStatus = "CUR";
def String partyType = "DEF";
def String caseStatus = "CLOSED";
def Case cse = pleaAgreementDocument.case;

def Where whereCaseSpecialStatus = new Where()
        .addEquals("status", caseSpecialStatusPleaStatus)
        .addEquals("case", cse);

def ArrayList<CaseSpecialStatus> pleaOfferCaseSpecialStatuses = DomainObject.find(CaseSpecialStatus.class, whereCaseSpecialStatus);

def Where whereParty = new Where()
        .addEquals("case", cse)
        .addNotEquals("case.status", caseStatus)
        .addIsNotNull("person")
        .addEquals("partyType", partyType)
        .addEquals("status", partyActiveStatus);

def ArrayList<Party> defendants = DomainObject.find(Party.class, whereParty);

logger.debug("case status: ${cse.caseStatus}");
logger.debug("case defendants found: ${defendants.size()}");
for (def Party defendant in defendants) {
    logger.debug("Processing case defendant ${defendant.title} defendant id:${defendant.id}");

    def Where whereOtherParty = new Where()
            .addNotEquals("case", cse)
            .addNotEquals("case.status", caseStatus)
            .addEquals("person", defendant.person)
            .addEquals("partyType", partyType)
            .addEquals("status", partyActiveStatus);

    def ArrayList<Long> defendantOtherPartiesIDs = DomainObject.find(Party.class, whereOtherParty, sel("id"));
    if (!defendantOtherPartiesIDs.isEmpty()) {
/*        defendantOtherPartiesIDs.each({ Long partyID ->
            logger.debug("defendantParty id:${partyID} case:${Party.get(partyID).case.caseStatus} caseID: ${Party.get(partyID).case.id}");
            logger.debug("isTargetCase: ${Condition.get("Case CaseStatus in Active, Review, Warrant, Original Indictment, Further Investigation Needed").isTrue(Party.get(partyID).case)}")
        });
        def ArrayList<Long> targetCases = defendantOtherPartiesIDs.findAll({ Long partyID ->
            Condition.get("Case CaseStatus in Active, Review, Warrant, Original Indictment, Further Investigation Needed").isTrue(Party.get(partyID).case)
        });
        logger.debug("target cases: ${targetCases.size()}");
        if (!targetCases.isEmpty()) {
            logger.debug("target cases found: adding case special status value");
            caseSpecialStatusValue += getCaseListStatuses(targetCases);
        }
        logger.debug("defendant other cases found: ${defendantOtherPartiesIDs.size()} ${defendantOtherPartiesIDs}");*/
        if (pleaOfferCaseSpecialStatuses.isEmpty()) {
            logger.debug("creating case special status");
            def CaseSpecialStatus newPleaOfferCaseSpecialStatus = new CaseSpecialStatus();
            pleaOfferCaseSpecialStatuses.add(newPleaOfferCaseSpecialStatus);
            cse.add(newPleaOfferCaseSpecialStatus);
        }
        for (def CaseSpecialStatus pleaOfferCaseSpecialStatus in pleaOfferCaseSpecialStatuses) {
            logger.debug("processing case special statuses");
            pleaOfferCaseSpecialStatus.setCase(cse);
            pleaOfferCaseSpecialStatus.setStatus(caseSpecialStatusPleaStatus);
            pleaOfferCaseSpecialStatus.setStartDate(caseSpecialStatusDate);
            pleaOfferCaseSpecialStatus.setValue(caseSpecialStatusValue);
            pleaOfferCaseSpecialStatus.setMemo(caseSpecialStatusValue);
        }
    } else {
        logger.debug("defendant other cases not found");
        for (def CaseSpecialStatus pleaOfferCaseSpecialStatus in pleaOfferCaseSpecialStatuses) {
            logger.debug("if case special status found, set end date");
            pleaOfferCaseSpecialStatus.setStatus(caseSpecialStatusPleaStatus);
            pleaOfferCaseSpecialStatus.setEndDate(caseSpecialStatusDate);
        }
    }

}
if (!pleaOfferCaseSpecialStatuses.isEmpty()) {
    DomainObject.saveOrUpdateAll(pleaOfferCaseSpecialStatuses);
    DomainObject.saveOrUpdate(cse);
    logger.debug("plea offer case special statuses saved");
}

private String getCaseListStatuses(List<Long> targetPartyIDs) {
    def String caseStatuses = "";
    for (def Long targetPartyID in targetPartyIDs) {
        caseStatuses += " " + Party.get(targetPartyID).case.caseStatus;
    }
    return caseStatuses;
}