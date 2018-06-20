package nl.irp.sepa.sdd;

import iso.std.iso._20022.tech.xsd.pain_008_001.*;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;
import static nl.irp.sepa.sdd.Utils.*;


/**
 * This document describes the Implementation Guidelines for the XML SEPA Direct Debit Initiation message
 * UNIFI (ISO20022) - “pain.008.001.02” in the Netherlands.

 * @author "Jasper Krijgsman <jasper@irp.nl>"
 *
 */
public class DirectDebitInitiation {

	private Document document = new Document();
	private CustomerDirectDebitInitiationV02 customerDirectDebitInitiationV02;
	private GroupHeader39 groupHeader;

	public DirectDebitInitiation() {
		customerDirectDebitInitiationV02 = new CustomerDirectDebitInitiationV02();
		document.setCstmrDrctDbtInitn(customerDirectDebitInitiationV02);
	}

	/**
	 * Set of characteristics shared by all individual transactions included in the message.
	 * @param msgId Point to point reference, assigned by the instructing party and sent to
	 * the next party in the chain, to unambiguously identify the message.
	 * @param name
	 * @param date
	 */
	public void buildGroupHeader(String msgId, String name, Date date) {
		groupHeader = new GroupHeader39();

		// if no msgId is given create one
		if(msgId==null)
			msgId = UUID.randomUUID().toString().replaceAll("-", "");
		checkArgument(msgId.length()<=35, "length of setMsgId is more than 35");
		checkArgument(msgId.length()>1, "length of setMsgId is less than 1");
		groupHeader.setMsgId(msgId);

		// Date and time at which the message was created.
		groupHeader.setCreDtTm( createXMLGregorianCalendar(date));

		// Number of individual transactions contained in the message.
		groupHeader.setNbOfTxs("0");

		//Total of all individual amounts included in the message.
		groupHeader.setCtrlSum(BigDecimal.ZERO);

		// Party that initiates the payment.
		groupHeader.setInitgPty( createParty(name) );

		customerDirectDebitInitiationV02.setGrpHdr(groupHeader);
	}

	public void write(OutputStream os) throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance(Document.class);
		Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        // The UTF-8 character encoding standard must be used in the UNIFI messages.
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        marshaller.marshal(new ObjectFactory().createDocument(document), os);
	}

	public PaymentInstruction paymentInstruction(
			String pmtInfId, Date reqdColltnDt,
			String creditor, SequenceType1Code type,
			String creditorCountry, List<String> addressLines,
			String creditorAccount, String creditorBic, String cdtrSchmeId) {

		PaymentInstruction paymentInstruction = new PaymentInstruction(
				pmtInfId, reqdColltnDt,
				creditor, type,
				creditorCountry, addressLines,
				creditorAccount, creditorBic, cdtrSchmeId);
		this.customerDirectDebitInitiationV02.getPmtInf().add(paymentInstruction.getPaymentInstructionInformation());
		return paymentInstruction;
	}

	public class PaymentInstruction {

		private PaymentInstructionInformation4 paymentInstructionInformation;

		/**
		 * collected from the debtor.
		 *
		 * @param pmtInfId
		 * @param reqdColltnDt Date and time at which the creditor requests that the amount of money is to be
		 * @param creditor
		 * @param type
		 * @param creditorCountry
		 * @param addressLines
		 * @param creditorAccount
		 * @param creditorBic
		 * @param cdtrSchmeId
		 */
		public PaymentInstruction(
				String pmtInfId, Date reqdColltnDt,
				String creditor, SequenceType1Code type,
				String creditorCountry, List<String> addressLines,
				String creditorAccount, String creditorBic, String cdtrSchmeId) {

			paymentInstructionInformation = new PaymentInstructionInformation4();

			// Unique identification, as assigned by a sending party, to
			// unambiguously identify the payment information group within the message.
			checkArgument(pmtInfId.length()<=35, "length of pmtInfId is more than 35");
			checkArgument(pmtInfId.length()>1, "length of pmtInfId is less than 1");
			paymentInstructionInformation.setPmtInfId(pmtInfId);

			// Specifies the means of payment that will be used to move the amount of money.
			// DD=DirectDebit
			paymentInstructionInformation.setPmtMtd(PaymentMethod2Code.DD);

			// TODO
			paymentInstructionInformation.setNbOfTxs("0");

			// TODO
			paymentInstructionInformation.setCtrlSum(BigDecimal.ZERO);

			// TODO
			paymentInstructionInformation.setPmtTpInf(makePaymentTypeInformation(type));

			// Date and time at which the creditor requests that the amount of money is to be
			// collected from the debtor.
			paymentInstructionInformation.setReqdColltnDt( createXMLGregorianCalendarDate(reqdColltnDt) );

			// Party to which an amount of money is due.
			paymentInstructionInformation.setCdtr( createParty(creditor, creditorCountry, addressLines) );
			// Unambiguous identification of the account of the creditor to which a credit entry will
			// be posted as a result of the payment transaction. Only IBAN is allowed.
			paymentInstructionInformation.setCdtrAcct( createAccount(creditorAccount) );

			paymentInstructionInformation.setCdtrAgt( createFinInstnId(creditorBic) );

			paymentInstructionInformation.setChrgBr(ChargeBearerType1Code.SLEV);

			if (StringUtils.isNotBlank(cdtrSchmeId)) {
				paymentInstructionInformation.setCdtrSchmeId(Utils.createIdParty(cdtrSchmeId));
			}



		}

        /**
         * @param instructionIdentification
         * @param endToEndIdentification
         * @param amount
         * @param mandateId
         * @param dateOfSignature
         * @param cdtrSchmeId
         * @param debtor
         * @param debtorIban
         * @param debtorBic
         * @param debtorCtry
         * @param debtorAdrLine
         * @param remittanceInformation
         * @return
         */
		public DirectDebitTransactionInformation9 addTransaction(
				String instructionIdentification, String endToEndIdentification,
				BigDecimal amount,
				String mandateId, LocalDate dateOfSignature, String cdtrSchmeId,
				String debtor, String debtorIban, String debtorBic,
				String debtorCtry, List<String> debtorAdrLine,
				String remittanceInformation) {

		    return addTransaction(instructionIdentification, endToEndIdentification, amount, mandateId, dateOfSignature, cdtrSchmeId, debtor, debtorIban, debtorBic, debtorCtry, debtorAdrLine, remittanceInformation, null, "OTHR");
		}

        /**
         * @param instructionIdentification
         * @param endToEndIdentification
         * @param amount
         * @param mandateId
         * @param dateOfSignature
         * @param cdtrSchmeId
         * @param debtor
         * @param debtorIban
         * @param debtorBic
         * @param debtorCtry
         * @param debtorAdrLine
         * @param remittanceInformation
         * @param debtorPreviousIban        le précédent iban du débiteur pour la même RUM (autorisation de prélèvement) s'il a changé et que c'est le premier prélèvement suivant ce changement d'iban
         * @param purposeCd
         * @return
         */
        public DirectDebitTransactionInformation9 addTransaction(
                String instructionIdentification, String endToEndIdentification,
                BigDecimal amount,
                String mandateId, LocalDate dateOfSignature, String cdtrSchmeId,
                String debtor, String debtorIban, String debtorBic,
                String debtorCtry, List<String> debtorAdrLine,
                String remittanceInformation,
                String debtorPreviousIban,
                String purposeCd) {

            DirectDebitTransactionInformation9 directDebitTransactionInformation = new DirectDebitTransactionInformation9();

            // Set of elements used to reference a payment instruction.
            directDebitTransactionInformation.setPmtId(createPaymentIdentification(instructionIdentification, endToEndIdentification));

            // Amount of money to be moved between the debtor and creditor, before deduction
            // of charges, expressed in the currency as ordered by the initiating party.
            directDebitTransactionInformation.setInstdAmt(createAmount(amount));


            directDebitTransactionInformation.setDrctDbtTx(t(mandateId, dateOfSignature, cdtrSchmeId));

            // Ajout du bloc pour le changement d'iban
            if (StringUtils.isNotBlank(debtorPreviousIban)) {
                // On modifie la valeur de AmdmntInd à true pour préciser qu'il s'agit d'un changement d'iban
                directDebitTransactionInformation.getDrctDbtTx().getMndtRltdInf().setAmdmntInd(true);

                // et on ajoute l'ancien iban utilisé dans le XML
                AmendmentInformationDetails6 amendmentInformationDetails6 = new AmendmentInformationDetails6();
                amendmentInformationDetails6.setOrgnlMndtId(debtorPreviousIban);
                directDebitTransactionInformation.getDrctDbtTx().getMndtRltdInf().setAmdmntInfDtls(amendmentInformationDetails6);
            }

            // Financial institution servicing an account for the debtor.
            directDebitTransactionInformation.setDbtrAgt(createFinInstnId(debtorBic));

            // Party that owes an amount of money to the (ultimate) creditor.
            directDebitTransactionInformation.setDbtr(createParty(debtor));
            directDebitTransactionInformation.setDbtrAcct(createAccount(debtorIban));

            if (StringUtils.isNotBlank(purposeCd)) {
                Purpose2Choice purpose = new Purpose2Choice();
                purpose.setCd("OTHR");
                directDebitTransactionInformation.setPurp(purpose);
            }

            directDebitTransactionInformation.setRmtInf(createRmtInf(remittanceInformation));

            paymentInstructionInformation.getDrctDbtTxInf().add(directDebitTransactionInformation);

            // TODO:
            BigDecimal ctrlSum = groupHeader.getCtrlSum();
            ctrlSum = ctrlSum.add(amount);
            groupHeader.setCtrlSum(ctrlSum);

            int nbOfTxs = Integer.parseInt(groupHeader.getNbOfTxs());
            nbOfTxs = nbOfTxs + 1;
            groupHeader.setNbOfTxs(String.valueOf(nbOfTxs));

            // TODO:
            BigDecimal ictrlSum = paymentInstructionInformation.getCtrlSum();
            ictrlSum = ictrlSum.add(amount);
            paymentInstructionInformation.setCtrlSum(ictrlSum);

            int inbOfTxs = Integer.parseInt(paymentInstructionInformation.getNbOfTxs());
            inbOfTxs = inbOfTxs + 1;
            paymentInstructionInformation.setNbOfTxs(String.valueOf(inbOfTxs));

            return directDebitTransactionInformation;
        }

		public PaymentInstructionInformation4 getPaymentInstructionInformation() {
			return paymentInstructionInformation;
		}


		private PaymentTypeInformation20 makePaymentTypeInformation(SequenceType1Code type) {
			// Payment Type Information
			PaymentTypeInformation20 paymentTypeInformation = new PaymentTypeInformation20();

			ServiceLevel8Choice serviceLevel8Choice = new ServiceLevel8Choice();
			serviceLevel8Choice.setCd("SEPA");//Vaste waarde 'SEPA'
			paymentTypeInformation.setSvcLvl(serviceLevel8Choice);

			LocalInstrument2Choice localInstrument = new LocalInstrument2Choice();
			localInstrument.setCd("CORE"); // "CORE" voor incasso's van particulieren
			paymentTypeInformation.setLclInstrm(localInstrument);

			//FRST eerste incasso binnen een serie op hetzelfde mandaat
			//RCUR vervolgincasso binnen hetzelfde mandaat
			//FNAL laatste incasso binnen hetzelfde mandaat
			//OOFF enkelvoudige incasso zonder repetering
			// Als de "Amendment indicator" (veld 2.50) op 'true' staat en de
			// "Original Debtor Agent"(veld 2.58) is "SMNDA" dan moet "FRST" gekozen	worden
			// Na een afwijzing van een "FRST" of "OOFF" moet een herhaling als "FRST" aangegeven worden
			// Als een "FRST" gestorneerd of geretourneerd wordt (alleen bij type "CORE") moet deze als "RCUR" ingestuurd worden
			// Als een "OOFF" gestorneerd of geretourneerd wordt (alleen bij type "CORE") kan deze alleen met een nieuw mandaat ingestuurd worden
			paymentTypeInformation.setSeqTp(type);

			return paymentTypeInformation;
		}

		private DirectDebitTransaction6 t(String mandateId, LocalDate dtOfSgntr, String cdtrSchmeId  ) {
			DirectDebitTransaction6 transaction = new DirectDebitTransaction6();

			MandateRelatedInformation6 mandateInf = new MandateRelatedInformation6();
			mandateInf.setMndtId(mandateId);
			mandateInf.setDtOfSgntr( createXMLGregorianCalendarDate(dtOfSgntr.toDate()));
			mandateInf.setAmdmntInd(false);
			transaction.setMndtRltdInf(mandateInf);

			if (StringUtils.isNotBlank(cdtrSchmeId)) {
				transaction.setCdtrSchmeId(createIdParty(cdtrSchmeId));
			}

			return transaction;
		}

	}
}
