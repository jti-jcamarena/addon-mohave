import com.sustain.DomainObject
import com.sustain.calendar.model.ScheduledEvent
import com.sustain.cases.model.CaseSpecialStatus;
import com.sustain.expression.Where
import com.sustain.util.DateUtil;


def ScheduledEvent scheduledEvent = _event;
def Case cse = scheduledEvent.case;
def String caseSpecialStatusPleaStatus = "PLEADEFCASES";
def Date caseSpecialStatusDate = DateUtil.today;

def Where whereCaseSpecialStatus = new Where()
        .addEquals("status", caseSpecialStatusPleaStatus)
        .addEquals("case", cse)
        .addIsNull("endDate");

def ArrayList<CaseSpecialStatus> pleaOfferCaseSpecialStatuses = DomainObject.find(CaseSpecialStatus.class, whereCaseSpecialStatus);
logger.debug("pleaOfferCaseSpecialStatuses: ${pleaOfferCaseSpecialStatuses.size()}")
for (def CaseSpecialStatus pleaOfferCaseSpecialStatus in pleaOfferCaseSpecialStatuses) {
    logger.debug("update end date")
    pleaOfferCaseSpecialStatus.setEndDate(caseSpecialStatusDate);
}

if (!pleaOfferCaseSpecialStatuses.isEmpty()) {
    logger.debug("saving")
    DomainObject.saveOrUpdateAll(pleaOfferCaseSpecialStatuses);
    DomainObject.saveOrUpdate(cse);
}