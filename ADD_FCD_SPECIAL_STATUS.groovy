import com.sustain.cases.model.Case;
import com.sustain.DomainObject;
import com.sustain.expression.Where;
import com.sustain.casenotes.model.CaseNote;
import com.sustain.cases.model.CaseSpecialStatus;
import com.sustain.util.DateUtil;

def CaseNote caseNote = _caseNote;
def String status = _specialStatus;
def Date date = DateUtil.today;
def Case cse = caseNote.case;
def String memo = "FCD Review Started";

def Where whereCaseSpecialStatus = new Where()
        .addEquals("status", status)
        .addEquals("case", cse)
        .addIsNull("endDate");

def ArrayList<CaseSpecialStatus> fdcCaseSpecialStatuses = DomainObject.find(CaseSpecialStatus.class, whereCaseSpecialStatus);

if (fdcCaseSpecialStatuses.isEmpty()) {
    logger.debug("creating case special status");
    def CaseSpecialStatus newFdcCaseSpecialStatus = new CaseSpecialStatus();
    fdcCaseSpecialStatuses.add(newFdcCaseSpecialStatus);
    cse.add(newFdcCaseSpecialStatus);
}
for (def CaseSpecialStatus fdcCaseSpecialStatus in fdcCaseSpecialStatuses) {
    logger.debug("processing case special statuses");
    fdcCaseSpecialStatus.setCase(cse);
    fdcCaseSpecialStatus.setStatus(status);
    fdcCaseSpecialStatus.setStartDate(date);
    fdcCaseSpecialStatus.setValue(memo);
    fdcCaseSpecialStatus.setMemo(memo);
}
if (!fdcCaseSpecialStatuses.isEmpty()) {
    DomainObject.saveOrUpdateAll(fdcCaseSpecialStatuses);
    DomainObject.saveOrUpdate(cse);
    logger.debug("case special statuses saved");
}