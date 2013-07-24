package nl.irp.sepa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class SEPACreditTransferTest extends XMLTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		HashMap<String, String> ns = new HashMap<String, String>();
		ns.put("ns", "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03");

		NamespaceContext ctx = new SimpleNamespaceContext(ns);
		XMLUnit.setXpathNamespaceContext(ctx);
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
	}
	
	@Test
	public void testABN() throws DatatypeConfigurationException, JAXBException, XpathException, SAXException, IOException {
		LocalDateTime today = new LocalDateTime("2013-04-02T14:52:09"); 
		SEPACreditTransfer transfer = new SEPACreditTransfer();
		
		transfer.buildGroupHeader("000001", "Klantnaam", today.toDate());
		
		transfer
			.paymentGroup("12345", new LocalDate("2013-04-19"), "Debiteur", "NL02ABNA0123456789", "ABNANL2A")
				.creditTransfer("Onze referentie: 123456", new BigDecimal("386.00"), "RABONL2U", "Crediteur", "NL44RABO0123456789", "Ref. 2012.0386");
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		transfer.write(stream);
		String xml = stream.toString("UTF-8");

		String example = Resources.toString( Resources.getResource("abn/pain.001.001.03 voorbeeldbestand.xml"), Charsets.UTF_8);
		assertXMLEqual(example, xml);
	}
	
	@Test
	public void testING() throws DatatypeConfigurationException, JAXBException, XpathException, SAXException, IOException {
		LocalDateTime today = new LocalDateTime("2013-04-02T14:52:09"); 
		SEPACreditTransfer transfer = new SEPACreditTransfer();
		
		transfer.buildGroupHeader("MSGID005", "IPNORGANIZTIONNAME", today.toDate());
		
		transfer
			.paymentGroup("PAYID001", new LocalDate("2013-04-19"), "NAAM Debtor", "NL28INGB0000000001", "INGBNL2A")
				.creditTransfer("E2EID001", new BigDecimal("1.01"), "INGBNL2A", "NAAM cdtr", "NL98INGB0000000002", "Ref. 2012.0386");
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		transfer.write(stream);
		String xml = stream.toString("UTF-8");

		String example = Resources.toString( Resources.getResource("ing/pain.001.001.03 voorbeeldbestand.xml"), Charsets.UTF_8);
		assertXMLEqual(example, xml);
	}

        @Test
	public void testMultiple() throws DatatypeConfigurationException, JAXBException, XpathException, SAXException, IOException {
		LocalDateTime today = new LocalDateTime("2013-06-28T15:57:09"); 
		SEPACreditTransfer transfer = new SEPACreditTransfer();
		
		transfer.buildGroupHeader("MSGID005", "My Organization", today.toDate());
		
                SEPACreditTransfer.PaymentGroup paymentGroup = transfer.paymentGroup("PAYID001", new LocalDate("2013-07-01"), "Gewinnabwicklung TST", "AT131490022010010999", "SPADATW1");
		paymentGroup.creditTransfer("E2EID001", new BigDecimal("100.55"), "INGDDEFFXXX", "Paul Testmann", "DE12500105170648489890", "Ihr Gewinn vom 25.05.2013");
                paymentGroup.creditTransfer("E2EID002", new BigDecimal("17.00"), "RBABCH22350", "Peter Testmann", "CH3908704016075473007", "Ihr Gewinn vom 01.06.2013");
                paymentGroup.creditTransfer("E2EID003", new BigDecimal("100.00"), "RABOBE22", "Thomas Testmann", "BE68844010370034", "Ihr Gewinn vom 05.06.2013");
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		transfer.write(stream);
		String xml = stream.toString("UTF-8");

		String example = Resources.toString( Resources.getResource("ing/pain.001.001.03 multiple.xml"), Charsets.UTF_8);
		assertXMLEqual(example, xml);
	}
        
        @Test
	public void testRapidMoneyTransfer() throws DatatypeConfigurationException, JAXBException, XpathException, SAXException, IOException {
		LocalDateTime today = new LocalDateTime("2013-06-28T15:57:09"); 
		SEPACreditTransfer transfer = new SEPACreditTransfer();
		
		transfer.buildGroupHeader("MSGID005", "My Organization", today.toDate());
		
                SEPACreditTransfer.PaymentGroup paymentGroup = transfer.paymentGroup("PAYID001", new LocalDate("2013-07-01"), "Gewinnabwicklung TST", "AT131490022010010999", "SPADATW1", true);
		paymentGroup.creditTransfer("E2EID001", new BigDecimal("100.55"), "INGDDEFFXXX", "Paul Testmann", "DE12500105170648489890", "Ihr Gewinn vom 25.05.2013");
                paymentGroup.creditTransfer("E2EID002", new BigDecimal("17.00"), "RBABCH22350", "Peter Testmann", "CH3908704016075473007", "Ihr Gewinn vom 01.06.2013");
                paymentGroup.creditTransfer("E2EID003", new BigDecimal("100.00"), "RABOBE22", "Thomas Testmann", "BE68844010370034", "Ihr Gewinn vom 05.06.2013");
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		transfer.write(stream);
		String xml = stream.toString("UTF-8");

		String example = Resources.toString( Resources.getResource("ing/pain.001.003.03 rapidMoneyTransfer.xml"), Charsets.UTF_8);
		assertXMLEqual(example, xml);
	}
        
        @Test
	public void testPainVersion02() throws DatatypeConfigurationException, JAXBException, XpathException, SAXException, IOException {
		LocalDateTime today = new LocalDateTime("2013-06-28T15:57:09"); 
		SEPACreditTransfer transfer = new SEPACreditTransfer(SEPACreditTransfer.VERSION_PAIN_001_002_02);
		
		transfer.buildGroupHeader("MSGID005", "My Organization", today.toDate());
		
                SEPACreditTransfer.PaymentGroup paymentGroup = transfer.paymentGroup("PAYID001", new LocalDate("2013-07-01"), "Gewinnabwicklung TST", "AT131490022010010999", "SPADATW1");
		paymentGroup.creditTransfer("E2EID001", new BigDecimal("100.55"), "INGDDEFFXXX", "Paul Testmann", "DE12500105170648489890", "Ihr Gewinn vom 25.05.2013");
                paymentGroup.creditTransfer("E2EID002", new BigDecimal("17.00"), "RBABCH22350", "Peter Testmann", "CH3908704016075473007", "Ihr Gewinn vom 01.06.2013");
                paymentGroup.creditTransfer("E2EID003", new BigDecimal("100.00"), "RABOBE22", "Thomas Testmann", "BE68844010370034", "Ihr Gewinn vom 05.06.2013");
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		transfer.write(stream);
		String xml = stream.toString("UTF-8");

		String example = Resources.toString( Resources.getResource("ing/pain.001.002.02 multiple.xml"), Charsets.UTF_8);
		assertXMLEqual(example, xml);
	}
        
        @Test
	public void testRefuseVersion02RapidMoneyTransfer() throws DatatypeConfigurationException, JAXBException, XpathException, SAXException, IOException {
		LocalDateTime today = new LocalDateTime("2013-06-28T15:57:09"); 
		SEPACreditTransfer transfer = new SEPACreditTransfer(SEPACreditTransfer.VERSION_PAIN_001_002_02);
		
		transfer.buildGroupHeader("MSGID005", "My Organization", today.toDate());
		
                boolean wasRefused = false;
                try {
                    SEPACreditTransfer.PaymentGroup paymentGroup = transfer.paymentGroup("PAYID001", new LocalDate("2013-07-01"), "Gewinnabwicklung TST", "AT131490022010010999", "SPADATW1", true);
                } catch (IllegalArgumentException e) {
                    System.out.println("Caught exception " + e.getClass().getName() + ": '" + e.getMessage() + "'");
                    wasRefused = true;
                }
		assertTrue(wasRefused);
	}

}
