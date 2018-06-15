sepa-pain - updated for GERMANY -- TESTED FOR FRANCE
=========

This is a fork of [https://github.com/poornerd/java-sepa](https://github.com/poornerd/java-sepa) which has specific adjustments for Deutsche financial institutions.

It has been very lightly adusted to work in France.

Testé pour des Virements Sepa avec le Crédit Mutuel. 
Testé pour des Prélèvements automatiques. 


The implementation is based on:
XML message for SEPA Credit Transfer Initiation Implementation Guidelines for the Netherlands
Version 5.0 – January 2012

supported messages:
* SEPA Credit Transfers (SCT): pain.001.001.03  (Customer Credit Transfer Initiation)
  Nieuwste versie: geldig vanaf november 2012
  [Implementation guidelines DNB V6.0 (pdf)](http://www.abnamro.nl/nl/images/Generiek/PDFs/020_Zakelijk/01_Betalingsverkeer/Betaalvereniging_IG_SEPA_Credit_Transfer_6-0.pdf)
  [Addendum ABN AMRO voor V6.0 (pdf)](http://www.abnamro.nl/nl/images/Generiek/PDFs/020_Zakelijk/01_Betalingsverkeer/Addendum_on_the_XML_Message_for_SEPA_Credit_Transfer_Initiation_version_6-0.pdf)

* SEPA message for Bank to Customer Statement (camt.053)
  [NVB IG Bank to Customer Statement (CAMT_053)](http://www.abnamro.nl/nl/images/Generiek/PDFs/020_Zakelijk/01_Betalingsverkeer/NVB_IG_Bank_to_Customer_Statement_\(CAMT_053\)_v0_99_final.pdf)

