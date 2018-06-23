package nl.irp.sepa;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import iso.std.iso._20022.tech.xsd.pain_008_001.SequenceType1Code;
import nl.irp.sepa.sdd.DirectDebitInitiation;
import nl.irp.sepa.sdd.DirectDebitInitiation.PaymentInstruction;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.jdom2.JDOMException;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

public class DirectDebitInitiationTest extends XMLTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		HashMap<String, String> ns = new HashMap<String, String>();
		ns.put("ns", "urn:iso:std:iso:20022:tech:xsd:pain.008.001.02");

		NamespaceContext ctx = new SimpleNamespaceContext(ns);
		XMLUnit.setXpathNamespaceContext(ctx);
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
	}
	
	@Test
	public void testABN() throws JAXBException, SAXException, IOException {
		LocalDateTime today = new LocalDateTime("2012-02-22T09:29:54"); 
		DirectDebitInitiation debitInitiation = new DirectDebitInitiation();
		
		debitInitiation.buildGroupHeader("1000004207", "Naam", today.toDate());
		
		String pmtInfId = "1000004207";
		PaymentInstruction paymentInstruction = debitInitiation
			.paymentInstruction(
				pmtInfId, new LocalDate(2012, 2, 21).toDate(),
				"Naam", SequenceType1Code.RCUR,
				"NL", ImmutableList.of("Dorpstraat 1", "Amsterdam"),
				"DE12345678901234567890", "ABNADEFFFRA", null);
		
		paymentInstruction.addTransaction(
			"01-E30220000000382012",     //InstrId
			"2000000038",                //EndToEndId
			new BigDecimal("1600.00"),
			"MANDAAT123456", new LocalDate("2010-09-05"), "NL89ZZZ011234567890",
			"FICO Customer account",
			"DE12345678901234567890", "NBAGDE3E",
			"DE", ImmutableList.of("123, ABC street", "32547 Frankfurt Germany"),
			"/INV/ 8/29/2011");
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		debitInitiation.write(stream);
		String xml = stream.toString("UTF-8");
		
		System.out.println(xml);

		String example = Resources.toString( Resources.getResource("abn/pain.008.001.02 voorbeeldbestand.xml"), Charsets.UTF_8);
		assertXMLEqual(example, xml);
	}
	
	@Test
	public void testING() throws JAXBException, SAXException, IOException {
		LocalDateTime today = new LocalDateTime("2012-02-22T09:29:54"); 
		DirectDebitInitiation debitInitiation = new DirectDebitInitiation();
		
		debitInitiation.buildGroupHeader("MSGID001", "IPNORGANISATIENAAM", today.toDate());
		
		String pmtInfId = "PAYID001";
		PaymentInstruction paymentInstruction = debitInitiation
			.paymentInstruction(
				pmtInfId, new LocalDate("2012-02-05").toDate(),
				"NAAM", SequenceType1Code.OOFF,
				"NL", ImmutableList.of("Dorpstraat 1", "Amsterdam"),
				"NL28INGB0000000001", "INGBNL2A", null);
		
		paymentInstruction.addTransaction(
			"01-E30220000000382012",     //InstrId
			"E2EID001",                //EndToEndId
			new BigDecimal("1.01"),
			"MANDAATIDNR001", new LocalDate("2011-12-31"), "NL89ZZZ011234567890",
			"NAAM",
			"NL98INGB0000000002", "INGBNL2A",
			"DE", ImmutableList.of("123, ABC street", "32547 Frankfurt Germany"),
			"Omschrijving / vrije tekst");
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		debitInitiation.write(stream);
		String xml = stream.toString("UTF-8");
		
		System.out.println(xml);

		String example = Resources.toString( Resources.getResource("ing/pain.008.001.02 voorbeeldbestand.xml"), Charsets.UTF_8);
		assertXMLEqual(example, xml);
	}

	@Test
	public void testPaymentWithIbanChanged() throws JAXBException, SAXException, IOException, JDOMException {
		LocalDateTime today = new LocalDateTime("2012-02-22T09:29:54");
		DirectDebitInitiation debitInitiation = new DirectDebitInitiation();

		debitInitiation.buildGroupHeader("MSGID001", "IPNORGANISATIENAAM", today.toDate());

		String pmtInfId = "PAYID001";
		PaymentInstruction paymentInstruction = debitInitiation
				.paymentInstruction(
						pmtInfId, new LocalDate("2012-02-05").toDate(),
						"NAAM", SequenceType1Code.OOFF,
						"NL", ImmutableList.of("Dorpstraat 1", "Amsterdam"),
						"NL28INGB0000000001", "INGBNL2A", null);

		paymentInstruction.addTransaction(
				"01-E30220000000382012",     //InstrId
				"E2EID001",                //EndToEndId
				new BigDecimal("1.01"),
				"MANDAATIDNR001", new LocalDate("2011-12-31"), "NL89ZZZ011234567890",
				"NAAM",
				"NL98INGB0000000002", "INGBNL2A",
				"DE", ImmutableList.of("123, ABC street", "32547 Frankfurt Germany"),
				"Omschrijving / vrije tekst",
				"NL98INGB0000000011",
				null);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		debitInitiation.writeWithXmlnsXsi(stream);
		String xml = stream.toString("UTF-8");

		System.out.println(xml);

		String example = Resources.toString( Resources.getResource("ing/pain.008.001.02 changeIban.xml"), Charsets.UTF_8);
		assertXMLEqual(example, xml);
	}

	@Test
	public void testCdtrSchmeIdInPaymentInstruction() throws Exception {
		LocalDateTime today = new LocalDateTime("2012-02-22T09:29:54");
		DirectDebitInitiation debitInitiation = new DirectDebitInitiation();

		debitInitiation.buildGroupHeader("MSGID001", "IPNORGANISATIENAAM", today.toDate());

		String pmtInfId = "PAYID001";
		PaymentInstruction paymentInstruction = debitInitiation
				.paymentInstruction(
						pmtInfId, new LocalDate("2012-02-05").toDate(),
						"NAAM", SequenceType1Code.OOFF,
						"NL", ImmutableList.of("Dorpstraat 1", "Amsterdam"),
						"NL28INGB0000000001", "INGBNL2A", "NL89ZZZ011234567890");

		paymentInstruction.addTransaction(
				"01-E30220000000382012",     //InstrId
				"E2EID001",                //EndToEndId
				new BigDecimal("1.01"),
				"MANDAATIDNR001",
				new LocalDate("2011-12-31"),
				null,
				"NAAM",
				"NL98INGB0000000002",
				"INGBNL2A",
				"DE",
				ImmutableList.of("123, ABC street", "32547 Frankfurt Germany"),
				"Omschrijving / vrije tekst",
				"NL98INGB0000000011",
				null);

		paymentInstruction.addTransaction(
				"01-E30220000000382013",     //InstrId
				"E2EID002",                //EndToEndId
				new BigDecimal("1.03"),
				"MANDAATIDNR001",
				new LocalDate("2011-12-31"),
				null,
				"NAAM",
				"NL98INGB0000000003",
				"INGBNL23",
				"DE",
				ImmutableList.of("123, ABC street", "32547 Frankfurt Germany"),
				"Omschrijving / vrije tekst",
				null,
				null);

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		debitInitiation.writeWithXmlnsXsi(stream);
		String xml = stream.toString("UTF-8");

        System.out.println(xml);

		String example = Resources.toString( Resources.getResource("ing/pain.008.001.02 cdtrSchmeId-paymentInstruction.xml"), Charsets.UTF_8);
		assertXMLEqual(example, xml);
	}

}
