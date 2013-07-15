package nl.irp.sepa;

import static com.google.common.base.Preconditions.checkArgument;
import static nl.irp.sepa.Utils.*;
import iso.std.iso._20022.tech.xsd.pain_001_001.ChargeBearerType1Code;
import iso.std.iso._20022.tech.xsd.pain_001_001.CreditTransferTransactionInformation10;
import iso.std.iso._20022.tech.xsd.pain_001_001.CustomerCreditTransferInitiationV03;
import iso.std.iso._20022.tech.xsd.pain_001_001.Document;
import iso.std.iso._20022.tech.xsd.pain_001_001.GroupHeader32;
import iso.std.iso._20022.tech.xsd.pain_001_001.LocalInstrument2Choice;
import iso.std.iso._20022.tech.xsd.pain_001_001.ObjectFactory;
import iso.std.iso._20022.tech.xsd.pain_001_001.PaymentIdentification1;
import iso.std.iso._20022.tech.xsd.pain_001_001.PaymentInstructionInformation3;
import iso.std.iso._20022.tech.xsd.pain_001_001.PaymentMethod3Code;
import iso.std.iso._20022.tech.xsd.pain_001_001.PaymentTypeInformation19;
import iso.std.iso._20022.tech.xsd.pain_001_001.ServiceLevel8Choice;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.joda.time.LocalDate;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The Customer SEPA Credit Transfer Initiation message is sent by the
 * initiating party to the debtor bank. It is used to request movement of funds
 * from the debtor account to a creditor account.
 *
 *
 * According to the Implementation Guidelines for the XML Customer Credit
 * Transfer Initiation message UNIFI (ISO20022) - "pain.001.001.03" in the
 * Netherlands.
 *
 * And: XML message for SEPA Credit Transfer Initiation Implementation
 * Guidelines for the Netherlands Version 5.0 – January 2012
 *
 * @author Jasper Krijgsman <jasper@irp.nl>
 */
public class SEPACreditTransfer {

    public static final int VERSION_PAIN_001_001_03 = 3;
    public static final int VERSION_PAIN_001_002_02 = 2;
    
    private Document document = new Document();
    private CustomerCreditTransferInitiationV03 customerCreditTransferInitiation;
    private GroupHeader32 groupHeader;
    private int version = VERSION_PAIN_001_001_03;

    public SEPACreditTransfer() {
        customerCreditTransferInitiation = new CustomerCreditTransferInitiationV03();
        document.setCstmrCdtTrfInitn(customerCreditTransferInitiation);
    }

    public void write(OutputStream os) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Document.class);
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        // The UTF-8 character encoding standard must be used in the UNIFI messages.
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        if (version == VERSION_PAIN_001_002_02) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            marshaller.marshal(new ObjectFactory().createDocument(document), bout);
            ByteArrayOutputStream convertedXml = convertPain03ToPain02(new ByteArrayInputStream(bout.toByteArray()));
            try {
                os.write(convertedXml.toByteArray());
            } catch (IOException ex) {
                Logger.getLogger(SEPACreditTransfer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            marshaller.marshal(new ObjectFactory().createDocument(document), os);
        }
    }
    
    /**
     * converts the given XML in format pain.001.001.03 into the format pain.001.002.02
     * @param xml XML as input stream
     * @return converted XML as output stream
     */
    public ByteArrayOutputStream convertPain03ToPain02(InputStream xml) {
        ByteArrayOutputStream bout = null;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            try {
                org.w3c.dom.Document doc = dBuilder.parse(xml);
                org.w3c.dom.Element root = doc.getDocumentElement();
                root.setAttribute("xmlns", "urn:swift:xsd:$pain.001.002.02");
                org.w3c.dom.Element container = null;
                NodeList cl = root.getElementsByTagName("CstmrCdtTrfInitn");
                if (cl.getLength() > 0) {
                    container = (org.w3c.dom.Element)cl.item(0);
                }
                if (container != null) {
                    doc.renameNode(container, null, "pain.001.001.02"); // ** rename node "CstmrCdtTrfInitn" to "pain.001.001.02" **
                    NodeList nl = container.getElementsByTagName("GrpHdr");
                    for (int i = 0; i < nl.getLength(); i++) {
                        org.w3c.dom.Element groupHeader = (org.w3c.dom.Element)nl.item(i);
                        // ** create a node "Grpg" with the content "MIXD" and add it before node "InitgPty" **
                        org.w3c.dom.Element grpg = doc.createElement("Grpg"); 
                        grpg.setTextContent("MIXD");
                        NodeList initiatingParty = groupHeader.getElementsByTagName("InitgPty");
                        if (initiatingParty.getLength() > 0) {
                            org.w3c.dom.Element party = (org.w3c.dom.Element)initiatingParty.item(0);
                            groupHeader.insertBefore(grpg, party);
                        }
                    }
                    NodeList paymentInfos = container.getElementsByTagName("PmtInf");
                    for (int i = 0; i < paymentInfos.getLength(); i++) {
                        // ** remove the nodes "NbOfTxs" and "CtrlSum" from payment infos **
                        org.w3c.dom.Element paymentInfo = (org.w3c.dom.Element)paymentInfos.item(i);
                        NodeList transactionNumbers = paymentInfo.getElementsByTagName("NbOfTxs");
                        for (int j = 0; j < transactionNumbers.getLength(); j++) {
                            paymentInfo.removeChild(transactionNumbers.item(j));
                        }
                        NodeList controlSums = paymentInfo.getElementsByTagName("CtrlSum");
                        for (int j = 0; j < controlSums.getLength(); j++) {
                            paymentInfo.removeChild(controlSums.item(j));
                        }
                    }
                }
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
                bout = new ByteArrayOutputStream();
		StreamResult xresult = new StreamResult(bout);
		transformer.transform(source, xresult);
            } catch (SAXException ex) {
                Logger.getLogger(SEPACreditTransfer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(SEPACreditTransfer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerConfigurationException ex) {
                Logger.getLogger(SEPACreditTransfer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (TransformerException ex) {
                Logger.getLogger(SEPACreditTransfer.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(SEPACreditTransfer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return bout;
    }

    /**
     * Group Header: This building block is mandatory and present once. It
     * contains elements such as Message Identification, Creation Date and Time,
     * Grouping Indicator. Set of characteristics shared by all individual
     * transactions included in the message.
     *
     * @param msgId Point to point reference, as assigned by the instructing
     * party
     * @param name Name of the party that initiates the payment.
     * @throws DatatypeConfigurationException
     */
    public void buildGroupHeader(String msgId, String name, Date date) {
        groupHeader = new GroupHeader32();
        // Point to point reference, as assigned by the instructing party, and sent to the next
        // party in the chain to unambiguously identify the message.
        // The instructing party has to make sure that MessageIdentification is unique per
        // instructed party for a pre-agreed period.
        // if no msgId is given create one
        if (msgId == null) {
            msgId = UUID.randomUUID().toString().replaceAll("-", "");
        }
        checkArgument(msgId.length() <= 35, "length of msgId is more than 35");
        checkArgument(msgId.length() > 1, "length of msgId is less than 1");
        groupHeader.setMsgId(msgId);

        // Date and time at which the message was created.
        groupHeader.setCreDtTm(createXMLGregorianCalendar(date));

        // Number of individual transactions contained in the message.
        groupHeader.setNbOfTxs("0");

        //Total of all individual amounts included in the message.
        groupHeader.setCtrlSum(BigDecimal.ZERO);

        // Party that initiates the payment.
        groupHeader.setInitgPty(createParty(name));

        customerCreditTransferInitiation.setGrpHdr(groupHeader);
    }

    /**
     * Payment Information: This building block is mandatory and repetitive. It
     * contains besides elements related to the debit side of the transaction,
     * such as Debtor and Payment Type Information, also one or several
     * Transaction Information Blocks.
     *
     * Set of characteristics that applies to the debit side of the payment
     * transactions included in the credit transfer initiation.
     *
     * @param pmtInfId Unique identification, as assigned by a sending party, to
     * unambiguously identify the payment information group within the message.
     * @param date This is the date on which the debtor's account is to be
     * debited.
     * @param debtorNm Party that owes an amount of money to the (ultimate)
     * creditor.
     * @param debtorAccountIBAN Unambiguous identification of the account of the
     * debtor to which a debit
     * @param isRapidMoneyTransfer <code>true</code> if the transactions of this
     * group shall be handled as rapid money transfer ("Eilueberweisung"),
     * default <code>false</code> entry will be made as a result of the
     * transaction.
     * @return
     * @throws DatatypeConfigurationException
     */
    public PaymentGroup paymentGroup(
            String pmtInfId, LocalDate reqdExctnDt,
            String debtorNm, String debtorAccountIBAN, String financialInstitutionBIC,
            boolean isRapidMoneyTransfer) {

        checkArgument(pmtInfId.length() <= 35, "length of pmtInfId is more than 35");
        checkArgument(pmtInfId.length() > 1, "length of pmtInfId is less than 1");


        PaymentInstructionInformation3 paymentInstructionInformation = new PaymentInstructionInformation3();
        //customerCreditTransferInitiation.getPmtInf().add(paymentInstructionInformation);

        // Unique identification, as assigned by a sending party, to unambiguously identify the
        // payment information group within the message.
        paymentInstructionInformation.setPmtInfId(pmtInfId);

        // Specifies the means of payment that will be used to move the amount of money.
        // Only ‘TRF’ is allowed.
        paymentInstructionInformation.setPmtMtd(PaymentMethod3Code.TRF);

        // Number of individual transactions contained in the payment information group.
        paymentInstructionInformation.setNbOfTxs("0");

        // Total of all individual amounts included in the group
        paymentInstructionInformation.setCtrlSum(BigDecimal.ZERO);

        // Payment Type Information
        PaymentTypeInformation19 paymentTypeInformation = new PaymentTypeInformation19();
        ServiceLevel8Choice serviceLevel8Choice = new ServiceLevel8Choice();
        String serviceLevelCode = "SEPA";
        if (isRapidMoneyTransfer) {
            serviceLevelCode = "URGP";
        }
        serviceLevel8Choice.setCd(serviceLevelCode);
        paymentTypeInformation.setSvcLvl(serviceLevel8Choice);
        paymentInstructionInformation.setPmtTpInf(paymentTypeInformation);

        // This is the date on which the debtor's account is to be debited. 
        paymentInstructionInformation.setReqdExctnDt(createXMLGregorianCalendarDate(reqdExctnDt.toDate()));

        // Party that owes an amount of money to the (ultimate) creditor.
        paymentInstructionInformation.setDbtr(createParty(debtorNm));

        // Unambiguous identification of the account of the debtor to which a debit entry will be
        // made as a result of the transaction.
        paymentInstructionInformation.setDbtrAcct(createAccount(debtorAccountIBAN));

        // Financial institution servicing an account for the debtor.
        paymentInstructionInformation.setDbtrAgt(createFinInstnId(financialInstitutionBIC));

        paymentInstructionInformation.setChrgBr(ChargeBearerType1Code.SLEV);

        customerCreditTransferInitiation.getPmtInf().add(paymentInstructionInformation);

        return new PaymentGroup(paymentInstructionInformation);
    }

    /**
     * Payment Information: This building block is mandatory and repetitive. It
     * contains besides elements related to the debit side of the transaction,
     * such as Debtor and Payment Type Information, also one or several
     * Transaction Information Blocks.
     *
     * Set of characteristics that applies to the debit side of the payment
     * transactions included in the credit transfer initiation.
     *
     * @param pmtInfId Unique identification, as assigned by a sending party, to
     * unambiguously identify the payment information group within the message.
     * @param date This is the date on which the debtor's account is to be
     * debited.
     * @param debtorNm Party that owes an amount of money to the (ultimate)
     * creditor.
     * @param debtorAccountIBAN Unambiguous identification of the account of the
     * debtor to which a debit entry will be made as a result of the
     * transaction.
     * @return
     * @throws DatatypeConfigurationException
     */
    public PaymentGroup paymentGroup(
            String pmtInfId, LocalDate reqdExctnDt,
            String debtorNm, String debtorAccountIBAN, String financialInstitutionBIC) {
        return paymentGroup(pmtInfId, reqdExctnDt, debtorNm, debtorAccountIBAN, financialInstitutionBIC, false);
    }

    /**
     * @return the version
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(int version) {
        this.version = version;
    }

    public class PaymentGroup {

        private PaymentInstructionInformation3 paymentInstructionInformation3;

        public PaymentGroup(PaymentInstructionInformation3 paymentInstructionInformation3) {
            this.paymentInstructionInformation3 = paymentInstructionInformation3;
        }

        /**
         * Set of elements used to provide information on the individual
         * transaction(s) included in the message.
         *
         * @param endToEndId Unique identification assigned by the initiating
         * party to unambiguously identify the transaction. This identification
         * is passed on, unchanged, throughout the entire end-to-end chain.
         * maxLength: 35
         * @param amount Amount of money to be moved between the debtor and
         * creditor, before deduction of charges, expressed in the currency as
         * ordered by the initiating party.
         * @param creditorfinancialInstitutionBic Financial institution
         * servicing an account for the creditor.
         * @param creditorNm Party to which an amount of money is due.
         * @param iban Unambiguous identification of the account of the creditor
         * to which a credit entry will be posted as a result of the payment
         * transaction.
         * @return
         *
         */
        public PaymentGroup creditTransfer(String endToEndId, BigDecimal amount,
                String creditorfinancialInstitutionBic,
                String creditorNm, String iban,
                String text) {

            CreditTransferTransactionInformation10 creditTransferTransactionInformation = new CreditTransferTransactionInformation10();

            // Unique identification as assigned by an instructing party for an instructed party to
            // unambiguously identify the instruction.
            PaymentIdentification1 paymentIdentification = new PaymentIdentification1();
            paymentIdentification.setEndToEndId(endToEndId);
            creditTransferTransactionInformation.setPmtId(paymentIdentification);

            // Amount of money to be moved between the debtor and creditor, before deduction of 
            // charges, expressed in the currency as ordered by the initiating party.
            creditTransferTransactionInformation.setAmt(createAmount(amount));

            // Only 'SLEV' is allowed. 
            //creditTransferTransactionInformation.setChrgBr(ChargeBearerType1Code.SLEV);

            // Financial institution servicing an account for the creditor.
            creditTransferTransactionInformation.setCdtrAgt(createFinInstnId(creditorfinancialInstitutionBic));

            // Party to which an amount of money is due.
            creditTransferTransactionInformation.setCdtr(createParty(creditorNm));

            // Unambiguous identification of the account of the creditor to which a credit entry will
            // be posted as a result of the payment transaction.
            creditTransferTransactionInformation.setCdtrAcct(createAccount(iban));

            creditTransferTransactionInformation.setRmtInf(createRmtInf(text));

            paymentInstructionInformation3.getCdtTrfTxInf().add(creditTransferTransactionInformation);

            // Control sum
            paymentInstructionInformation3.setCtrlSum(paymentInstructionInformation3.getCtrlSum().add(amount));
            groupHeader.setCtrlSum(groupHeader.getCtrlSum().add(amount));

            // Number of transactions
            paymentInstructionInformation3.setNbOfTxs(String.valueOf(paymentInstructionInformation3.getCdtTrfTxInf().size()));
            Integer nbOfTxs = Integer.parseInt(groupHeader.getNbOfTxs());
            nbOfTxs = nbOfTxs + 1;
            groupHeader.setNbOfTxs(nbOfTxs.toString());

            return this;
        }
    }
}
