import com.sustain.document.model.Document;
import com.sustain.cases.model.Case;
import com.sustain.DomainObject;
import com.sustain.expression.Where;
import com.sustain.cases.model.Party;
import com.sustain.cases.model.CaseSpecialStatus
import com.sustain.lookuplist.model.LookupItem
import com.sustain.person.model.PersonSpecialStatus;
import com.sustain.util.DateUtil;
import com.sustain.casenotes.model.CaseNote;
import com.sustain.notebook.model.NoteColor;
import com.sustain.SharingLevel;
import com.sustain.condition.model.Condition;

def List<String> targetCaseStatuses = Arrays.asList("WARRA", "ACTIV", "ORIND", "REVIE", "FURTH");
def Document pleaAgreementDocument = _document;
def String caseSpecialStatusPleaStatus = "PLEADEFCASES";
def Date caseSpecialStatusDate = DateUtil.today;
def String caseSpecialStatusValue = "Please verify plea includes ALL the Defendant's cases. YOU MUST review Defendant case involvements before proceeding.";
def String partyActiveStatus = "CUR";
def String partyType = "DEF";
def String caseStatus = "CLOSED";
def ArrayList<String> excludedCaseStatuses = Arrays.asList("CLOSED", "DESTR");
DomainObject.find(LookupItem.class, "lookupList.name", "CASE_STATUS")
        .findAll({ LookupItem it -> it.label?.toLowerCase()?.contains("closed") })
        .each({ LookupItem it -> excludedCaseStatuses.add(it.code) });
logger.debug("excludedCaseStatuses:${excludedCaseStatuses}")
def Case cse = pleaAgreementDocument.case;
def NoteColor color = NoteColor.WHITE;
def SharingLevel sharingLevel = SharingLevel.PUBLIC;
def String title = "Plea Offer Review";
def String content = caseSpecialStatusValue;
def Date noteDate = caseSpecialStatusDate;
def String noteType = "PO";
def CaseNote note;
def String personSpecialStatusCategory = "";
def ArrayList<PersonSpecialStatus> personSpecialStatuses = new ArrayList<>();
def ArrayList<Person> personsToSave = new ArrayList<>();

def Where whereCaseSpecialStatus = new Where()
        .addEquals("status", caseSpecialStatusPleaStatus)
        .addEquals("case", cse)
        .addIsNull("endDate");

def ArrayList<CaseSpecialStatus> pleaOfferCaseSpecialStatuses = DomainObject.find(CaseSpecialStatus.class, whereCaseSpecialStatus);

def Where whereParty = new Where()
        .addEquals("case", cse)
        .addIsNotNull("person")
        .addEquals("partyType", partyType)
        .addEquals("status", partyActiveStatus);

def ArrayList<Party> defendants = DomainObject.find(Party.class, whereParty);
logger.debug("1:pleaOfferCaseSpecialStatuses:${pleaOfferCaseSpecialStatuses}")
logger.debug("2:defendants:${defendants}")
for (def Party defendant in defendants) {
    personsToSave.add(defendant.person);
    def PersonSpecialStatus personSpecialStatus;
    def Where whereOtherParty = new Where()
            .addNotEquals("case", cse)
            .addNotInOrNull("case.status", excludedCaseStatuses)
            .addEquals("person", defendant.person)
            .addEquals("partyType", partyType)
            .addEquals("status", partyActiveStatus);

    def ArrayList<Long> defendantOtherPartiesIDs = DomainObject.find(Party.class, whereOtherParty, sel("id"));
    logger.debug("3:defendantOtherPartiesIDs:${defendantOtherPartiesIDs}")

    if (hasPartyOtherInvolvementsOfTargetCaseType(defendant, targetCaseStatuses)) {
        personSpecialStatus = createPersonSpecialStatus(defendant, caseSpecialStatusValue, personSpecialStatusCategory, caseSpecialStatusDate);
        if (personSpecialStatus != null) {
            personSpecialStatuses.add(personSpecialStatus);
        }
    }

    for (def Long defendantOtherPartiesID in defendantOtherPartiesIDs) {
        logger.debug("defendantOtherPartiesID status: ${Party.get(defendantOtherPartiesID).case.status} ${Party.get(defendantOtherPartiesID).case}")
        def Party otherParty = Party.get(defendantOtherPartiesID);
        if ( !hasPersonSpecialStatus(otherParty,caseSpecialStatusValue) && hasPartyOtherInvolvementsOfTargetCaseType(otherParty, targetCaseStatuses)) {
            createPersonSpecialStatus(otherParty, caseSpecialStatusValue, personSpecialStatusCategory, caseSpecialStatusDate);
            if (personSpecialStatus != null && !personSpecialStatuses.contains(personSpecialStatus)) {
                personSpecialStatuses.add(personSpecialStatus);
            }
        }
    }
    if (!defendantOtherPartiesIDs.isEmpty()) {
        logger.debug("4:")
        if (pleaOfferCaseSpecialStatuses.isEmpty()) {
            logger.debug("5:")
            def CaseSpecialStatus newPleaOfferCaseSpecialStatus = new CaseSpecialStatus();
            pleaOfferCaseSpecialStatuses.add(newPleaOfferCaseSpecialStatus);
            cse.add(newPleaOfferCaseSpecialStatus);
        }
        for (def CaseSpecialStatus pleaOfferCaseSpecialStatus in pleaOfferCaseSpecialStatuses) {
            logger.debug("6:")
            pleaOfferCaseSpecialStatus.setCase(cse);
            pleaOfferCaseSpecialStatus.setStatus(caseSpecialStatusPleaStatus);
            pleaOfferCaseSpecialStatus.setStartDate(caseSpecialStatusDate);
            pleaOfferCaseSpecialStatus.setValue(caseSpecialStatusValue);
            pleaOfferCaseSpecialStatus.setMemo(caseSpecialStatusValue);
        }
        if (!hasCaseNote(cse, color, sharingLevel, title, content, noteDate, noteType)) {
            logger.debug("7:")
            note = saveCaseNote(cse, color, sharingLevel, title, content, noteDate, noteType)
            cse.add(note);
        }
    } else {
        logger.debug("8:")
        for (def CaseSpecialStatus pleaOfferCaseSpecialStatus in pleaOfferCaseSpecialStatuses) {
            logger.debug("9:")
            pleaOfferCaseSpecialStatus.setStatus(caseSpecialStatusPleaStatus);
            pleaOfferCaseSpecialStatus.setStartDate(caseSpecialStatusDate);
            pleaOfferCaseSpecialStatus.setEndDate(caseSpecialStatusDate);
        }
    }

}

if(!personSpecialStatuses.isEmpty()){
    DomainObject.saveOrUpdateAll(personSpecialStatuses);
}

if (!personsToSave.isEmpty()){
    DomainObject.saveOrUpdateAll(personsToSave);
}

if (!pleaOfferCaseSpecialStatuses.isEmpty()) {
    logger.debug("10:")
    DomainObject.saveOrUpdateAll(pleaOfferCaseSpecialStatuses);
}
if (note != null && note.content != null) {
    logger.debug("11:")
    DomainObject.saveOrUpdate(note);
}
logger.debug("12:")
DomainObject.saveOrUpdate(cse);
logger.debug("13:")

private boolean hasPersonSpecialStatus(Party party, String message){
    def Where wherePersonSpecialStatus = new Where()
        .addEquals("associatedPerson", party.person)
        .addEquals("value", message)
        .addIsNull("endDate");

    if (!DomainObject.find(PersonSpecialStatus.class, wherePersonSpecialStatus).isEmpty()){
        return true;
    } else return false;
}

private PersonSpecialStatus createPersonSpecialStatus(Party party, String message, String category, Date date) {
    def Person person = party?.person;
    if (person != null) {
        def PersonSpecialStatus personSpecialStatus = new PersonSpecialStatus();
        personSpecialStatus.setAssociatedPerson(person);
        personSpecialStatus.setCategory(category);
        personSpecialStatus.setValue(message);
        personSpecialStatus.setStartDate(date);
        person.add(personSpecialStatus);
        return personSpecialStatus;
    } else return null;
}

private boolean hasPartyOtherInvolvementsOfTargetCaseType(Party party, List<String> targetCaseStatuses) {
    if (targetCaseStatuses.contains(party.case.status)) {
        return true;
    } else return false;
}

private String getCaseListStatuses(List<Long> targetPartyIDs) {
    def String caseStatuses = "";
    for (def Long targetPartyID in targetPartyIDs) {
        caseStatuses += " " + Party.get(targetPartyID).case.caseStatus;
    }
    return caseStatuses;
}

private Boolean hasCaseNote(Case cse, NoteColor color, SharingLevel sharingLevel, String title, String content, Date noteDate, String type) {
    def Where whereNote = new Where()
            .addEquals("case", cse)
            .addEquals("color", color)
            .addEquals("sharingLevel", sharingLevel)
            .addEquals("title", title)
            .addEquals("content", content)
            .addEquals("noteDate", noteDate)
            .addEquals("type", type)

    def ArrayList<CaseNote> notes = DomainObject.find(CaseNote.class, whereNote);
    if (notes.isEmpty()) {
        return false;
    } else {
        return true;
    };
}

private CaseNote saveCaseNote(Case cse, NoteColor color, SharingLevel sharingLevel, String title, String content, Date noteDate, String type) {

    def CaseNote note = new CaseNote();
    note.setColor(color);
    note.setSharingLevel(sharingLevel);
    note.setTitle(title);
    note.setContent(content);
    note.setNoteDate(noteDate);
    note.setType(type);
    note.setCase(cse);
    return note;
}