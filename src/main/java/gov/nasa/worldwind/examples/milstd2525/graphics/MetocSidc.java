/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.examples.milstd2525.graphics;

/**
 * SIDC constants for graphics in the "Meteorological and Oceanographic" scheme (MIL-STD-2525C Appendix C). The
 * constants in this interface are "masked" SIDCs. All fields except Scheme, Category, and Function ID are filled with
 * hyphens. (The other fields do not identity a type of graphic, they modify the graphic.)
 *
 * @author pabercrombie
 * @version $Id: MetocSidc.java 442 2012-03-10 19:48:24Z pabercrombie $
 */
public interface MetocSidc {
    /**
     * Low pressure center.
     */
    String AMPHC_PRS_LOWCTR = "WAS-PL----P----";
    /**
     * Cyclone center.
     */
    String AMPHC_PRS_LOWCTR_CYC = "WAS-PC----P----";
    /**
     * Tropopause low.
     */
    String AMPHC_PRS_LOWCTR_TROPLW = "WAS-PLT---P----";
    /**
     * High pressure center.
     */
    String AMPHC_PRS_HGHCTR = "WAS-PH----P----";
    /**
     * Anticyclone center.
     */
    String AMPHC_PRS_HGHCTR_ACYC = "WAS-PA----P----";
    /**
     * Tropopause high.
     */
    String AMPHC_PRS_HGHCTR_TROPHG = "WAS-PHT---P----";
    /**
     * Frontal systems.
     */
    String AMPHC_PRS_FRNSYS = "WA-DPF-----L---";
    /**
     * Cold front.
     */
    String AMPHC_PRS_FRNSYS_CLDFRN = "WA-DPFC----L---";
    /**
     * Upper cold front.
     */
    String AMPHC_PRS_FRNSYS_CLDFRN_UPP = "WA-DPFCU---L---";
    /**
     * Cold frontogenesis.
     */
    String AMPHC_PRS_FRNSYS_CLDFRN_FRGS = "WA-DPFC-FG-L---";
    /**
     * Cold frontolysis.
     */
    String AMPHC_PRS_FRNSYS_CLDFRN_FRLS = "WA-DPFC-FY-L---";
    /**
     * Warm front.
     */
    String AMPHC_PRS_FRNSYS_WRMFRN = "WA-DPFW----L---";
    /**
     * Upper warm front.
     */
    String AMPHC_PRS_FRNSYS_WRMFRN_UPP = "WA-DPFWU---L---";
    /**
     * Warm frontogenesis.
     */
    String AMPHC_PRS_FRNSYS_WRMFRN_FRGS = "WA-DPFW-FG-L---";
    /**
     * Warm frontolysis.
     */
    String AMPHC_PRS_FRNSYS_WRMFRN_FRLS = "WA-DPFW-FY-L---";
    /**
     * Occluded front.
     */
    String AMPHC_PRS_FRNSYS_OCD = "WA-DPFO----L---";
    /**
     * Upper occluded front.
     */
    String AMPHC_PRS_FRNSYS_OCD_UPP = "WA-DPFOU---L---";
    /**
     * Occluded frontolysis.
     */
    String AMPHC_PRS_FRNSYS_OCD_FRLS = "WA-DPFO-FY-L---";
    /**
     * Stationary front.
     */
    String AMPHC_PRS_FRNSYS_STAT = "WA-DPFS----L---";
    /**
     * Upper stationary front.
     */
    String AMPHC_PRS_FRNSYS_STAT_UPP = "WA-DPFSU---L---";
    /**
     * Stationary frontogenesis.
     */
    String AMPHC_PRS_FRNSYS_STAT_FRGS = "WA-DPFS-FG-L---";
    /**
     * Stationary frontolysis.
     */
    String AMPHC_PRS_FRNSYS_STAT_FRLS = "WA-DPFS-FY-L---";
    /**
     * Pressure systems, trough axis.
     */
    String AMPHC_PRS_LNE_TRUAXS = "WA-DPXT----L---";
    /**
     * Ridge axis.
     */
    String AMPHC_PRS_LNE_RDGAXS = "WA-DPXR----L---";
    /**
     * Severe squall line.
     */
    String AMPHC_PRS_LNE_SSL = "WA-DPXSQ---L---";
    /**
     * Instability line.
     */
    String AMPHC_PRS_LNE_ISTB = "WA-DPXIL---L---";
    /**
     * Shear line.
     */
    String AMPHC_PRS_LNE_SHA = "WA-DPXSH---L---";
    /**
     * Inter-tropical convergance zone.
     */
    String AMPHC_PRS_LNE_ITCZ = "WA-DPXITCZ-L---";
    /**
     * Convergance line.
     */
    String AMPHC_PRS_LNE_CNGLNE = "WA-DPXCV---L---";
    /**
     * Inter-tropical discontinuity.
     */
    String AMPHC_PRS_LNE_ITD = "WA-DPXITD--L---";
    /**
     * Turbulence - light.
     */
    String AMPHC_TRB_LIT = "WAS-TL----P----";
    /**
     * Turbulence - moderate.
     */
    String AMPHC_TRB_MOD = "WAS-TM----P----";
    /**
     * Turbulence - severe.
     */
    String AMPHC_TRB_SVR = "WAS-TS----P----";
    /**
     * Turbulence - extreme.
     */
    String AMPHC_TRB_EXT = "WAS-TE----P----";
    /**
     * Mountain waves.
     */
    String AMPHC_TRB_MNTWAV = "WAS-T-MW--P----";
    /**
     * Clear icing - light.
     */
    String AMPHC_ICG_CLR_LIT = "WAS-ICL---P----";
    /**
     * Clear icing - moderate.
     */
    String AMPHC_ICG_CLR_MOD = "WAS-ICM---P----";
    /**
     * Clear icing - severe.
     */
    String AMPHC_ICG_CLR_SVR = "WAS-ICS---P----";
    /**
     * Rime icing - light.
     */
    String AMPHC_ICG_RIME_LIT = "WAS-IRL---P----";
    /**
     * Rime icing - moderate.
     */
    String AMPHC_ICG_RIME_MOD = "WAS-IRM---P----";
    /**
     * Rime icing - severe.
     */
    String AMPHC_ICG_RIME_SVR = "WAS-IRS---P----";
    /**
     * Mixed icing - light.
     */
    String AMPHC_ICG_MIX_LIT = "WAS-IML---P----";
    /**
     * Mixed icing - moderate.
     */
    String AMPHC_ICG_MIX_MOD = "WAS-IMM---P----";
    /**
     * Mixed icing - severe.
     */
    String AMPHC_ICG_MIX_SVR = "WAS-IMS---P----";
    /**
     * Calm winds.
     */
    String AMPHC_WND_CALM = "WAS-WC----P----";
    /**
     * Wind plot.
     */
    String AMPHC_WND_PLT = "WAS-WP----P----";
    /**
     * Jet stream.
     */
    String AMPHC_WND_JTSM = "WA-DWJ-----L---";
    /**
     * Stream line.
     */
    String AMPHC_WND_SMLNE = "WA-DWS-----L---";
    /**
     * Clear sky.
     */
    String AMPHC_CUDCOV_SYM_SKC = "WAS-CCCSCSP----";
    /**
     * Few coverage.
     */
    String AMPHC_CUDCOV_SYM_FEW = "WAS-CCCSFCP----";
    /**
     * Scattered coverage.
     */
    String AMPHC_CUDCOV_SYM_SCT = "WAS-CCCSSCP----";
    /**
     * Broken coverage.
     */
    String AMPHC_CUDCOV_SYM_BKN = "WAS-CCCSBCP----";
    /**
     * Overcast coverage.
     */
    String AMPHC_CUDCOV_SYM_OVC = "WAS-CCCSOCP----";
    /**
     * Sky totally or partially obscured.
     */
    String AMPHC_CUDCOV_SYM_STOPO = "WAS-CCCSOBP----";
    /**
     * Rain - intermittent light.
     */
    String AMPHC_WTH_RA_INMLIT = "WAS-WSR-LIP----";
    /**
     * Rain - continuous light.
     */
    String AMPHC_WTH_RA_INMLIT_CTSLIT = "WAS-WSR-LCP----";
    /**
     * Rain - intermittent moderate.
     */
    String AMPHC_WTH_RA_INMMOD = "WAS-WSR-MIP----";
    /**
     * Rain - continuous moderate.
     */
    String AMPHC_WTH_RA_INMMOD_CTSMOD = "WAS-WSR-MCP----";
    /**
     * Rain - intermittent heavy.
     */
    String AMPHC_WTH_RA_INMHVY = "WAS-WSR-HIP----";
    /**
     * Rain - continuous heavy.
     */
    String AMPHC_WTH_RA_INMHVY_CTSHVY = "WAS-WSR-HCP----";
    /**
     * Freezing rain - light.
     */
    String AMPHC_WTH_FZRA_LIT = "WAS-WSRFL-P----";
    /**
     * Freezing rain - moderate/heavy.
     */
    String AMPHC_WTH_FZRA_MODHVY = "WAS-WSRFMHP----";
    /**
     * Rain showers - light.
     */
    String AMPHC_WTH_RASWR_LIT = "WAS-WSRSL-P----";
    /**
     * Rain showers - moderate/heavy.
     */
    String AMPHC_WTH_RASWR_MODHVY = "WAS-WSRSMHP----";
    /**
     * Rain showers - torrential.
     */
    String AMPHC_WTH_RASWR_TOR = "WAS-WSRST-P----";
    /**
     * Drizzle - intermittent light.
     */
    String AMPHC_WTH_DZ_INMLIT = "WAS-WSD-LIP----";
    /**
     * Drizzle - continuous light.
     */
    String AMPHC_WTH_DZ_INMLIT_CTSLIT = "WAS-WSD-LCP----";
    /**
     * Drizzle - intermittent moderate.
     */
    String AMPHC_WTH_DZ_INMMOD = "WAS-WSD-MIP----";
    /**
     * Drizzle - continuous moderate.
     */
    String AMPHC_WTH_DZ_INMMOD_CTSMOD = "WAS-WSD-MCP----";
    /**
     * Drizzle - intermittent heavy.
     */
    String AMPHC_WTH_DZ_INMHVY = "WAS-WSD-HIP----";
    /**
     * Drizzle - continuous heavy.
     */
    String AMPHC_WTH_DZ_INMHVY_CTSHVY = "WAS-WSD-HCP----";
    /**
     * Freezing drizzle - light.
     */
    String AMPHC_WTH_FZDZ_LIT = "WAS-WSDFL-P----";
    /**
     * Freezing drizzle - moderate/heavy.
     */
    String AMPHC_WTH_FZDZ_MODHVY = "WAS-WSDFMHP----";
    /**
     * Rain or drizzle and snow - light.
     */
    String AMPHC_WTH_RASN_RDSLIT = "WAS-WSM-L-P----";
    /**
     * Rain or drizzle and snow - moderate/heavy.
     */
    String AMPHC_WTH_RASN_RDSMH = "WAS-WSM-MHP----";
    /**
     * Rain and snow showers - light.
     */
    String AMPHC_WTH_RASN_SWRLIT = "WAS-WSMSL-P----";
    /**
     * Rain and snow showers - moderate/heavy.
     */
    String AMPHC_WTH_RASN_SWRMOD = "WAS-WSMSMHP----";
    /**
     * Snow - intermittent light.
     */
    String AMPHC_WTH_SN_INMLIT = "WAS-WSS-LIP----";
    /**
     * Snow - continuous light.
     */
    String AMPHC_WTH_SN_INMLIT_CTSLIT = "WAS-WSS-LCP----";
    /**
     * Snow - intermittent moderate.
     */
    String AMPHC_WTH_SN_INMMOD = "WAS-WSS-MIP----";
    /**
     * Snow - continuous moderate.
     */
    String AMPHC_WTH_SN_INMMOD_CTSMOD = "WAS-WSS-MCP----";
    /**
     * Snow - intermittent heavy.
     */
    String AMPHC_WTH_SN_INMHVY = "WAS-WSS-HIP----";
    /**
     * Snow - continuous heavy.
     */
    String AMPHC_WTH_SN_INMHVY_CTSHVY = "WAS-WSS-HCP----";
    /**
     * Blowing snow - light/moderate.
     */
    String AMPHC_WTH_SN_BLSNLM = "WAS-WSSBLMP----";
    /**
     * Blowing snow - heavy.
     */
    String AMPHC_WTH_SN_BLSNHY = "WAS-WSSBH-P----";
    /**
     * Snow grains.
     */
    String AMPHC_WTH_SG = "WAS-WSSG--P----";
    /**
     * Snow showers - light.
     */
    String AMPHC_WTH_SSWR_LIT = "WAS-WSSSL-P----";
    /**
     * Snow showers - moderate/heavy.
     */
    String AMPHC_WTH_SSWR_MODHVY = "WAS-WSSSMHP----";
    /**
     * Hail - light not associated with thunder.
     */
    String AMPHC_WTH_HL_LIT = "WAS-WSGRL-P----";
    /**
     * Hail - moderate/heavy not associated with thunder.
     */
    String AMPHC_WTH_HL_MODHVY = "WAS-WSGRMHP----";
    /**
     * Ice crystals (diamond dust).
     */
    String AMPHC_WTH_IC = "WAS-WSIC--P----";
    /**
     * Ice pellets - light.
     */
    String AMPHC_WTH_PE_LIT = "WAS-WSPLL-P----";
    /**
     * Ice pellets - moderate.
     */
    String AMPHC_WTH_PE_MOD = "WAS-WSPLM-P----";
    /**
     * Ice pellets - heavy.
     */
    String AMPHC_WTH_PE_HVY = "WAS-WSPLH-P----";
    /**
     * Thunderstorm - no precipitation.
     */
    String AMPHC_WTH_STMS_TS = "WAS-WST-NPP----";
    /**
     * Thunderstorm light to moderate with rain/snow - no hail.
     */
    String AMPHC_WTH_STMS_TSLMNH = "WAS-WSTMR-P----";
    /**
     * Thunderstorm heavy with rain/snow - no hail.
     */
    String AMPHC_WTH_STMS_TSHVNH = "WAS-WSTHR-P----";
    /**
     * Thunderstorm light to moderate - with hail.
     */
    String AMPHC_WTH_STMS_TSLMWH = "WAS-WSTMH-P----";
    /**
     * Thunderstorm heavy - with hail.
     */
    String AMPHC_WTH_STMS_TSHVWH = "WAS-WSTHH-P----";
    /**
     * Funnel cloud (tornado/waterspout).
     */
    String AMPHC_WTH_STMS_FC = "WAS-WST-FCP----";
    /**
     * Squall.
     */
    String AMPHC_WTH_STMS_SQL = "WAS-WST-SQP----";
    /**
     * Lightning.
     */
    String AMPHC_WTH_STMS_LTG = "WAS-WST-LGP----";
    /**
     * Fog - shallow patches.
     */
    String AMPHC_WTH_FG_SHWPTH = "WAS-WSFGPSP----";
    /**
     * Fog -shallow continuous.
     */
    String AMPHC_WTH_FG_SHWCTS = "WAS-WSFGCSP----";
    /**
     * Fog - patchy.
     */
    String AMPHC_WTH_FG_PTHY = "WAS-WSFGP-P----";
    /**
     * Fog - sky visible.
     */
    String AMPHC_WTH_FG_SKYVSB = "WAS-WSFGSVP----";
    /**
     * Fog - sky obscured.
     */
    String AMPHC_WTH_FG_SKYOBD = "WAS-WSFGSOP----";
    /**
     * Fog - freezing, sky visible.
     */
    String AMPHC_WTH_FG_FZSV = "WAS-WSFGFVP----";
    /**
     * Fog - freezing, sky not visible.
     */
    String AMPHC_WTH_FG_FZSNV = "WAS-WSFGFOP----";
    /**
     * Mist.
     */
    String AMPHC_WTH_MIST = "WAS-WSBR--P----";
    /**
     * Smoke.
     */
    String AMPHC_WTH_FU = "WAS-WSFU--P----";
    /**
     * Haze.
     */
    String AMPHC_WTH_HZ = "WAS-WSHZ--P----";
    /**
     * Dust/sand storm - light to moderate.
     */
    String AMPHC_WTH_DTSD_LITMOD = "WAS-WSDSLMP----";
    /**
     * Dust/sand storm - severe.
     */
    String AMPHC_WTH_DTSD_SVR = "WAS-WSDSS-P----";
    /**
     * Dust devil.
     */
    String AMPHC_WTH_DTSD_DTDVL = "WAS-WSDD--P----";
    /**
     * Blowing dust or sand.
     */
    String AMPHC_WTH_DTSD_BLDTSD = "WAS-WSDB--P----";
    /**
     * Tropical depression.
     */
    String AMPHC_WTH_TPLSYS_TROPDN = "WAS-WSTSD-P----";
    /**
     * Tropical storm.
     */
    String AMPHC_WTH_TPLSYS_TROPSM = "WAS-WSTSS-P----";
    /**
     * Hurricane/typhoon.
     */
    String AMPHC_WTH_TPLSYS_HC = "WAS-WSTSH-P----";
    /**
     * Tropical storm wind areas and date/time labels.
     */
    String AMPHC_WTH_TPLSYS_TSWADL = "WA-DWSTSWA--A--";
    /**
     * Volcanic eruption.
     */
    String AMPHC_WTH_VOLERN = "WAS-WSVE--P----";
    /**
     * Volcanic ash.
     */
    String AMPHC_WTH_VOLERN_VOLASH = "WAS-WSVA--P----";
    /**
     * Tropopause level.
     */
    String AMPHC_WTH_TROPLV = "WAS-WST-LVP----";
    /**
     * Freezing level.
     */
    String AMPHC_WTH_FZLVL = "WAS-WSF-LVP----";
    /**
     * Precipitation of unknown type and intensity.
     */
    String AMPHC_WTH_POUTAI = "WAS-WSUKP-P----";
    /**
     * Instrument flight rule (IFR).
     */
    String AMPHC_BDAWTH_IFR = "WA-DBAIF----A--";
    /**
     * Marginal visual flight rule (MVFR).
     */
    String AMPHC_BDAWTH_MVFR = "WA-DBAMV----A--";
    /**
     * Turbulence.
     */
    String AMPHC_BDAWTH_TRB = "WA-DBATB----A--";
    /**
     * Icing.
     */
    String AMPHC_BDAWTH_ICG = "WA-DBAI-----A--";
    /**
     * Liquid precipitation - non-convective continuous or intermittent.
     */
    String AMPHC_BDAWTH_LPNCI = "WA-DBALPNC--A--";
    /**
     * Liquid precipitation - convective.
     */
    String AMPHC_BDAWTH_LPNCI_LPC = "WA-DBALPC---A--";
    /**
     * Freezing/frozen precipitation.
     */
    String AMPHC_BDAWTH_FZPPN = "WA-DBAFP----A--";
    /**
     * Thunderstorms.
     */
    String AMPHC_BDAWTH_TS = "WA-DBAT-----A--";
    /**
     * Fog.
     */
    String AMPHC_BDAWTH_FG = "WA-DBAFG----A--";
    /**
     * Dust or sand.
     */
    String AMPHC_BDAWTH_DTSD = "WA-DBAD-----A--";
    /**
     * Operator-defined freeform.
     */
    String AMPHC_BDAWTH_ODFF = "WA-DBAFF----A--";
    /**
     * Isobar - surface.
     */
    String AMPHC_ISP_ISB = "WA-DIPIB---L---";
    /**
     * Contour - upper air.
     */
    String AMPHC_ISP_CTUR = "WA-DIPCO---L---";
    /**
     * Isotherm.
     */
    String AMPHC_ISP_IST = "WA-DIPIS---L---";
    /**
     * Isotach.
     */
    String AMPHC_ISP_ISH = "WA-DIPIT---L---";
    /**
     * Isodrosotherm.
     */
    String AMPHC_ISP_ISD = "WA-DIPID---L---";
    /**
     * Thickness.
     */
    String AMPHC_ISP_THK = "WA-DIPTH---L---";
    /**
     * Operator-defined freeform.
     */
    String AMPHC_ISP_ODFF = "WA-DIPFF---L---";
    /** Surface dry without cracks or appreciable dust or loose sand. */
//    final String AMPHC_STOG_WOSMIC_SUFDRY = "WAS-GND-NCP----";
    /**
     * Surface moist.
     */
    String AMPHC_STOG_WOSMIC_SUFMST = "WAS-GNM---P----";
    /**
     * Surface wet, standing water in small or large pools.
     */
    String AMPHC_STOG_WOSMIC_SUFWET = "WAS-GNW-SWP----";
    /**
     * Surface flooded.
     */
    String AMPHC_STOG_WOSMIC_SUFFLD = "WAS-GNFL--P----";
    /**
     * Surface frozen.
     */
    String AMPHC_STOG_WOSMIC_SUFFZN = "WAS-GNFZ--P----";
    /**
     * Glaze (thin ice) on ground.
     */
    String AMPHC_STOG_WOSMIC_GLZGRD = "WAS-GNG-TIP----";
    /**
     * Loose dry dust or sand not covering ground completely.
     */
    String AMPHC_STOG_WOSMIC_LDNCGC = "WAS-GNLDN-P----";
    /**
     * Thin loose dry dust or sand covering ground completely.
     */
    String AMPHC_STOG_WOSMIC_TLDCGC = "WAS-GNLDTCP----";
    /**
     * Moderate/thick loose dry dust or sand covering ground completely.
     */
    String AMPHC_STOG_WOSMIC_MLDCGC = "WAS-GNLDMCP----";
    /**
     * Extremely dry with cracks.
     */
    String AMPHC_STOG_WOSMIC_EXTDWC = "WAS-GNDEWCP----";
    /**
     * Predominately ice covered.
     */
    String AMPHC_STOG_WSMIC_PDMIC = "WAS-GSI---P----";
    /**
     * Compact or wet snow (with or without ice) covering less than one-half of ground.
     */
    String AMPHC_STOG_WSMIC_CWSNLH = "WAS-GSSCL-P----";
    /**
     * Compact or wet snow (with or without ice) covering at least one-half ground, but ground not completely covered.
     */
    String AMPHC_STOG_WSMIC_CSNALH = "WAS-GSSCH-P----";
    /**
     * Even layer of compact or wet snow covering ground completely.
     */
    String AMPHC_STOG_WSMIC_ELCSCG = "WAS-GSSCCEP----";
    /**
     * Uneven layer of compact or wet snow covering ground completely.
     */
    String AMPHC_STOG_WSMIC_ULCSCG = "WAS-GSSCCUP----";
    /**
     * Loose dry snow covering less than one-half of ground.
     */
    String AMPHC_STOG_WSMIC_LDSNLH = "WAS-GSSLL-P----";
    /**
     * Loose dry snow covering at least one-half ground, but ground not completely covered.
     */
    String AMPHC_STOG_WSMIC_LDSALH = "WAS-GSSLH-P----";
    /**
     * Even layer of loose dry snow covering ground completely.
     */
    String AMPHC_STOG_WSMIC_ELDSCG = "WAS-GSSLCEP----";
    /**
     * Uneven layer of loose dry snow covering ground completely.
     */
    String AMPHC_STOG_WSMIC_ULDSCG = "WAS-GSSLCUP----";
    /**
     * Snow covering ground completely; deep drifts.
     */
    String AMPHC_STOG_WSMIC_SCGC = "WAS-GSSDC-P----";
    /**
     * Icebergs.
     */
    String OCA_ISYS_IB = "WOS-IB----P----";
    /**
     * Many icebergs.
     */
    String OCA_ISYS_IB_MNY = "WOS-IBM---P----";
    /**
     * Belts and strips.
     */
    String OCA_ISYS_IB_BAS = "WOS-IBBS--P----";
    /**
     * Iceberg -general.
     */
    String OCA_ISYS_IB_GNL = "WOS-IBG---P----";
    /**
     * Many icebergs -general.
     */
    String OCA_ISYS_IB_MNYGNL = "WOS-IBMG--P----";
    /**
     * Bergy bit.
     */
    String OCA_ISYS_IB_BB = "WOS-IBBB--P----";
    /**
     * Many bergy bits.
     */
    String OCA_ISYS_IB_MNYBB = "WOS-IBBBM-P----";
    /**
     * Growler.
     */
    String OCA_ISYS_IB_GWL = "WOS-IBGL--P----";
    /**
     * Many growlers.
     */
    String OCA_ISYS_IB_MNYGWL = "WOS-IBGLM-P----";
    /**
     * Floeberg.
     */
    String OCA_ISYS_IB_FBG = "WOS-IBF---P----";
    /**
     * Ice island.
     */
    String OCA_ISYS_IB_II = "WOS-IBII--P----";
    /**
     * Bergy water.
     */
    String OCA_ISYS_ICN_BW = "WOS-ICWB--P----";
    /**
     * Water with radar targets.
     */
    String OCA_ISYS_ICN_WWRT = "WOS-ICWR--P----";
    /**
     * Ice free.
     */
    String OCA_ISYS_ICN_IF = "WOS-ICIF--P----";
    /**
     * Convergence.
     */
    String OCA_ISYS_DYNPRO_CNG = "WOS-IDC---P----";
    /**
     * Divergence.
     */
    String OCA_ISYS_DYNPRO_DVG = "WOS-IDD---P----";
    /**
     * Shearing or shear zone.
     */
    String OCA_ISYS_DYNPRO_SHAZ = "WOS-IDS---P----";
    /**
     * Ice drift (direction).
     */
    String OCA_ISYS_DYNPRO_ID = "WO-DIDID---L---";
    /**
     * Sea ice.
     */
    String OCA_ISYS_SI = "WOS-II----P----";
    /**
     * Ice thickness (observed).
     */
    String OCA_ISYS_SI_ITOBS = "WOS-IITM--P----";
    /**
     * Ice thickness (estimated).
     */
    String OCA_ISYS_SI_ITEST = "WOS-IITE--P----";
    /**
     * Melt puddles or flooded ice.
     */
    String OCA_ISYS_SI_MPOFI = "WOS-IIP---P----";
    /**
     * Limit of visual observation.
     */
    String OCA_ISYS_LMT_LOVO = "WO-DILOV---L---";
    /**
     * Limit of undercast.
     */
    String OCA_ISYS_LMT_LOU = "WO-DILUC---L---";
    /**
     * Limit of radar observation.
     */
    String OCA_ISYS_LMT_LORO = "WO-DILOR---L---";
    /**
     * Observed ice edge or boundary.
     */
    String OCA_ISYS_LMT_OIEOB = "WO-DILIEO--L---";
    /**
     * Estimated ice edge or boundary.
     */
    String OCA_ISYS_LMT_EIEOB = "WO-DILIEE--L---";
    /**
     * Ice edge or boundary from radar.
     */
    String OCA_ISYS_LMT_IEOBFR = "WO-DILIER--L---";
    /**
     * Openings in the ice, cracks.
     */
    String OCA_ISYS_OITI_CRK = "WO-DIOC----L---";
    /**
     * Cracks at a specific location.
     */
    String OCA_ISYS_OITI_CRKASL = "WO-DIOCS---L---";
    /**
     * Lead.
     */
    String OCA_ISYS_OITI_LED = "WO-DIOL----L---";
    /**
     * Frozen lead.
     */
    String OCA_ISYS_OITI_FZLED = "WO-DIOLF---L---";
    /**
     * Snow cover.
     */
    String OCA_ISYS_SC = "WOS-ISC---P----";
    /**
     * Sastrugi (with orientation).
     */
    String OCA_ISYS_SC_SWO = "WOS-ISS---P----";
    /**
     * Ridges or hummocks.
     */
    String OCA_ISYS_TOPFTR_HUM = "WOS-ITRH--P----";
    /**
     * Rafting.
     */
    String OCA_ISYS_TOPFTR_RFTG = "WOS-ITR---P----";
    /**
     * Jammed brash barrier.
     */
    String OCA_ISYS_TOPFTR_JBB = "WOS-ITBB--P----";
    /**
     * Soundings.
     */
    String OCA_HYDGRY_DPH_SNDG = "WOS-HDS---P----";
    /**
     * Depth curve.
     */
    String OCA_HYDGRY_DPH_CRV = "WO-DHDDL---L---";
    /**
     * Depth contour.
     */
    String OCA_HYDGRY_DPH_CTUR = "WO-DHDDC---L---";
    /**
     * Depth area.
     */
    String OCA_HYDGRY_DPH_ARA = "WO-DHDDA----A--";
    /**
     * Coastline.
     */
    String OCA_HYDGRY_CSTHYD_CSTLN = "WO-DHCC----L---";
    /**
     * Island.
     */
    String OCA_HYDGRY_CSTHYD_ISND = "WO-DHCI-----A--";
    /**
     * Beach.
     */
    String OCA_HYDGRY_CSTHYD_BEH = "WO-DHCB-----A--";
    /**
     * Water.
     */
    String OCA_HYDGRY_CSTHYD_H2O = "WO-DHCW-----A--";
    /**
     * Foreshore.
     */
    String OCA_HYDGRY_CSTHYD_FSH1_FSH2 = "WO-DHCF----L---";
    /**
     * Foreshore.
     */
    String OCA_HYDGRY_CSTHYD_FSH1_FSH3 = "WO-DHCF-----A--";
    /**
     * Berths (onshore).
     */
    String OCA_HYDGRY_PRTHBR_PRT_BRHSO = "WOS-HPB-O-P----";
    /**
     * Berths (anchor).
     */
    String OCA_HYDGRY_PRTHBR_PRT_BRHSA = "WOS-HPB-A-P----";
    /**
     * Anchorage.
     */
    String OCA_HYDGRY_PRTHBR_PRT_ANCRG1 = "WOS-HPBA--P----";
    /**
     * Anchorage.
     */
    String OCA_HYDGRY_PRTHBR_PRT_ANCRG2 = "WO-DHPBA---L---";
    /**
     * Anchorage.
     */
    String OCA_HYDGRY_PRTHBR_PRT_ANCRG3 = "WO-DHPBA----A--";
    /**
     * Call in point.
     */
    String OCA_HYDGRY_PRTHBR_PRT_CIP = "WOS-HPCP--P----";
    /**
     * Pier/wharf/quay.
     */
    String OCA_HYDGRY_PRTHBR_PRT_PWQ = "WO-DHPBP---L---";
    /**
     * Fishing harbor.
     */
    String OCA_HYDGRY_PRTHBR_FSG_FSGHBR = "WOS-HPFH--P----";
    /**
     * Fish stakes/traps/weirs.
     */
    String OCA_HYDGRY_PRTHBR_FSG_FSTK1 = "WOS-HPFS--P----";
    /**
     * Fish stakes/traps/weirs.
     */
    String OCA_HYDGRY_PRTHBR_FSG_FSTK2 = "WOS-HPFS---L---";
    /**
     * Fish stakes/traps/weirs.
     */
    String OCA_HYDGRY_PRTHBR_FSG_FSTK3 = "WOS-HPFF----A--";
    /**
     * Drydock.
     */
    String OCA_HYDGRY_PRTHBR_FAC_DDCK = "WO-DHPMD----A--";
    /**
     * Landing place.
     */
    String OCA_HYDGRY_PRTHBR_FAC_LNDPLC = "WOS-HPML--P----";
    /**
     * Offshore loading facility.
     */
    String OCA_HYDGRY_PRTHBR_FAC_OSLF1 = "WO-DHPMO--P----";
    /**
     * Offshore loading facility.
     */
    String OCA_HYDGRY_PRTHBR_FAC_OSLF2 = "WO-DHPMO---L---";
    /**
     * Offshore loading facility.
     */
    String OCA_HYDGRY_PRTHBR_FAC_OSLF3 = "WO-DHPMO----A--";
    /**
     * Ramp (above water).
     */
    String OCA_HYDGRY_PRTHBR_FAC_RAMPAW = "WO-DHPMRA--L---";
    /**
     * Ramp (below water).
     */
    String OCA_HYDGRY_PRTHBR_FAC_RAMPBW = "WO-DHPMRB--L---";
    /**
     * Landing ring.
     */
    String OCA_HYDGRY_PRTHBR_FAC_LNDRNG = "WOS-HPM-R-P----";
    /**
     * Ferry crossing.
     */
    String OCA_HYDGRY_PRTHBR_FAC_FRYCSG = "WOS-HPM-FC-L---";
    /**
     * Cable ferry crossing.
     */
    String OCA_HYDGRY_PRTHBR_FAC_CFCSG = "WOS-HPM-CC-L---";
    /**
     * Dolphin.
     */
    String OCA_HYDGRY_PRTHBR_FAC_DOPN = "WOS-HPD---P----";
    /**
     * Breakwater/groin/jetty (above water).
     */
    String OCA_HYDGRY_PRTHBR_SHRLNE_BWGJAW = "WO-DHPSPA--L---";
    /**
     * Breakwater/groin/jetty (below water).
     */
    String OCA_HYDGRY_PRTHBR_SHRLNE_BWGJBW = "WO-DHPSPB--L---";
    /**
     * Seawall.
     */
    String OCA_HYDGRY_PRTHBR_SHRLNE_SW = "WO-DHPSPS--L---";
    /**
     * Beacon.
     */
    String OCA_HYDGRY_ATN_BCN = "WOS-HABA--P----";
    /**
     * Buoy default.
     */
    String OCA_HYDGRY_ATN_BUOY = "WOS-HABB--P----";
    /**
     * Marker.
     */
    String OCA_HYDGRY_ATN_MRK = "WOS-HABM--P----";
    /**
     * Perches/stakes.
     */
    String OCA_HYDGRY_ATN_PRH1_PRH2 = "WOS-HABP--P----";
    /**
     * Perches/stakes.
     */
    String OCA_HYDGRY_ATN_PRH1_PRH3 = "WO-DHABP----A--";
    /**
     * Light.
     */
    String OCA_HYDGRY_ATN_LIT = "WOS-HAL---P----";
    /**
     * Leading line.
     */
    String OCA_HYDGRY_ATN_LDGLNE = "WO-DHALLA--L---";
    /**
     * Light vessel/lightship.
     */
    String OCA_HYDGRY_ATN_LITVES = "WOS-HALV--P----";
    /**
     * Lighthouse.
     */
    String OCA_HYDGRY_ATN_LITHSE = "WOS-HALH--P----";
    /**
     * Rock submergered.
     */
    String OCA_HYDGRY_DANHAZ_RCKSBM = "WOS-HHRS--P----";
    /**
     * Rock awashed.
     */
    String OCA_HYDGRY_DANHAZ_RCKAWD = "WOS-HHRA--P----";
    /**
     * Underwater danger/hazard.
     */
    String OCA_HYDGRY_DANHAZ_UH2DAN = "WO-DHHD-----A--";
    /**
     * Foul ground.
     */
    String OCA_HYDGRY_DANHAZ_FLGRD1_FLGRD2 = "WOS-HHDF--P----";
    /**
     * Foul ground.
     */
    String OCA_HYDGRY_DANHAZ_FLGRD1_FLGRD3 = "WO-DHHDF----A--";
    /**
     * Kelp/seaweed.
     */
    String OCA_HYDGRY_DANHAZ_KLP1_KLP2 = "WO-DHHDK--P----";
    /**
     * Kelp/seaweed.
     */
    String OCA_HYDGRY_DANHAZ_KLP1_KLP3 = "WO-DHHDK----A--";
    /**
     * Mine - naval (doubtful).
     */
    String OCA_HYDGRY_DANHAZ_MNENAV_DBT = "WOS-HHDMDBP----";
    /**
     * Mine - naval (definite).
     */
    String OCA_HYDGRY_DANHAZ_MNENAV_DEFN = "WOS-HHDMDFP----";
    /**
     * Snags/stumps.
     */
    String OCA_HYDGRY_DANHAZ_SNAG = "WOS-HHDS--P----";
    /**
     * Wreck (uncovers).
     */
    String OCA_HYDGRY_DANHAZ_WRK_UCOV = "WOS-HHDWA-P----";
    /**
     * Wreck (submerged).
     */
    String OCA_HYDGRY_DANHAZ_WRK_SBM = "WOS-HHDWB-P----";
    /**
     * Breakers.
     */
    String OCA_HYDGRY_DANHAZ_BRKS = "WO-DHHDB---L---";
    /**
     * Reef.
     */
    String OCA_HYDGRY_DANHAZ_REEF = "WOS-HHDR---L---";
    /**
     * Eddies/overfalls/tide rips.
     */
    String OCA_HYDGRY_DANHAZ_EOTR = "WOS-HHDE--P----";
    /**
     * Discolored water.
     */
    String OCA_HYDGRY_DANHAZ_DCDH2O = "WO-DHHDD----A--";
    /**
     * Sand.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_SD = "WOS-BFC-S-P----";
    /**
     * Mud.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_MUD = "WOS-BFC-M-P----";
    /**
     * Clay.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_CLAY = "WOS-BFC-CLP----";
    /**
     * Silt.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_SLT = "WOS-BFC-SIP----";
    /**
     * Stones.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_STNE = "WOS-BFC-STP----";
    /**
     * Gravel.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_GVL = "WOS-BFC-G-P----";
    /**
     * Pebbles.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_PBL = "WOS-BFC-P-P----";
    /**
     * Cobbles.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_COBL = "WOS-BFC-CBP----";
    /**
     * Rock.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_RCK = "WOS-BFC-R-P----";
    /**
     * Coral.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_CRL = "WOS-BFC-COP----";
    /**
     * Shell.
     */
    String OCA_HYDGRY_BTMFAT_BTMCHR_SHE = "WOS-BFC-SHP----";
    /**
     * Qualifying terms, fine.
     */
    String OCA_HYDGRY_BTMFAT_QLFYTM_FNE = "WOS-BFQ-F-P----";
    /**
     * Qualifying terms, medum.
     */
    String OCA_HYDGRY_BTMFAT_QLFYTM_MDM = "WOS-BFQ-M-P----";
    /**
     * Qualifying terms, coarse.
     */
    String OCA_HYDGRY_BTMFAT_QLFYTM_CSE = "WOS-BFQ-C-P----";
    /**
     * Water turbulence.
     */
    String OCA_HYDGRY_TDECUR_H2OTRB = "WOS-TCCW--P----";
    /**
     * Current flow - ebb.
     */
    String OCA_HYDGRY_TDECUR_EBB = "WO-DTCCCFE-L---";
    /**
     * Current flow - flood.
     */
    String OCA_HYDGRY_TDECUR_FLOOD = "WO-DTCCCFF-L---";
    /**
     * Tide data point.
     */
    String OCA_HYDGRY_TDECUR_TDEDP = "WOS-TCCTD-P----";
    /**
     * Tide gauge.
     */
    String OCA_HYDGRY_TDECUR_TDEG = "WOS-TCCTG-P----";
    /**
     * Bioluminescence, vdr level 1-2.
     */
    String OCA_OCNGRY_BIOLUM_VDR1_2 = "WO-DOBVA----A--";
    /**
     * Bioluminescence, vdr level 2-3.
     */
    String OCA_OCNGRY_BIOLUM_VDR2_3 = "WO-DOBVB----A--";
    /**
     * Bioluminescence, vdr level 3-4.
     */
    String OCA_OCNGRY_BIOLUM_VDR3_4 = "WO-DOBVC----A--";
    /**
     * Bioluminescence, vdr level 4-5.
     */
    String OCA_OCNGRY_BIOLUM_VDR4_5 = "WO-DOBVD----A--";
    /**
     * Bioluminescence, vdr level 5-6.
     */
    String OCA_OCNGRY_BIOLUM_VDR5_6 = "WO-DOBVE----A--";
    /**
     * Bioluminescence, vdr level 6-7.
     */
    String OCA_OCNGRY_BIOLUM_VDR6_7 = "WO-DOBVF----A--";
    /**
     * Bioluminescence, vdr level 7-8.
     */
    String OCA_OCNGRY_BIOLUM_VDR7_8 = "WO-DOBVG----A--";
    /**
     * Bioluminescence, vdr level 8-9.
     */
    String OCA_OCNGRY_BIOLUM_VDR8_9 = "WO-DOBVH----A--";
    /**
     * Bioluminescence, vdr level 9-10.
     */
    String OCA_OCNGRY_BIOLUM_VDR9_0 = "WO-DOBVI----A--";
    /**
     * Flat.
     */
    String OCA_OCNGRY_BEHSPE_FLT = "WO-DBSF-----A--";
    /**
     * Gentle.
     */
    String OCA_OCNGRY_BEHSPE_GTL = "WO-DBSG-----A--";
    /**
     * Moderate.
     */
    String OCA_OCNGRY_BEHSPE_MOD = "WO-DBSM-----A--";
    /**
     * Steep.
     */
    String OCA_OCNGRY_BEHSPE_STP = "WO-DBST-----A--";
    /**
     * Miw-bottom sediments, solid rock.
     */
    String OCA_GPHY_MNEWBD_MIWBS_SLDRCK = "WO-DGMSR----A--";
    /**
     * Miw-bottom sediments, clay.
     */
    String OCA_GPHY_MNEWBD_MIWBS_CLAY = "WO-DGMSC----A--";
    /**
     * Very coarse sand.
     */
    String OCA_GPHY_MNEWBD_MIWBS_VCSESD = "WO-DGMSSVS--A--";
    /**
     * Miw-bottom sediments, coarse sand.
     */
    String OCA_GPHY_MNEWBD_MIWBS_CSESD = "WO-DGMSSC---A--";
    /**
     * Miw-bottom sediments, medium sand.
     */
    String OCA_GPHY_MNEWBD_MIWBS_MDMSD = "WO-DGMSSM---A--";
    /**
     * Miw-bottom sediments, fine sand.
     */
    String OCA_GPHY_MNEWBD_MIWBS_FNESD = "WO-DGMSSF---A--";
    /**
     * Miw-bottom sediments, very fine sand.
     */
    String OCA_GPHY_MNEWBD_MIWBS_VFNESD = "WO-DGMSSVF--A--";
    /**
     * Miw-bottom sediments, very fine silt.
     */
    String OCA_GPHY_MNEWBD_MIWBS_VFNSLT = "WO-DGMSIVF--A--";
    /**
     * Miw-bottom sediments, file silt.
     */
    String OCA_GPHY_MNEWBD_MIWBS_FNESLT = "WO-DGMSIF---A--";
    /**
     * Miw-bottom sediments, medium silt.
     */
    String OCA_GPHY_MNEWBD_MIWBS_MDMSLT = "WO-DGMSIM---A--";
    /**
     * Miw-bottom sediments, coarse silt.
     */
    String OCA_GPHY_MNEWBD_MIWBS_CSESLT = "WO-DGMSIC---A--";
    /**
     * Boulders.
     */
    String OCA_GPHY_MNEWBD_MIWBS_BLDS = "WO-DGMSB----A--";
    /**
     * Cobbles, oyster shells.
     */
    String OCA_GPHY_MNEWBD_MIWBS_COBLOS = "WO-DGMS-CO--A--";
    /**
     * Pebbles, shells.
     */
    String OCA_GPHY_MNEWBD_MIWBS_PBLSHE = "WO-DGMS-PH--A--";
    /**
     * Sand and shells.
     */
    String OCA_GPHY_MNEWBD_MIWBS_SD_SHE = "WO-DGMS-SH--A--";
    /**
     * Miw-bottom sediments, land.
     */
    String OCA_GPHY_MNEWBD_MIWBS_LND = "WO-DGML-----A--";
    /**
     * No data.
     */
    String OCA_GPHY_MNEWBD_MIWBS_NODAT = "WO-DGMN-----A--";
    /**
     * Bottom roughness, smooth.
     */
    String OCA_GPHY_MNEWBD_BTMRGN_SMH = "WO-DGMRS----A--";
    /**
     * Bottom roughness, moderate.
     */
    String OCA_GPHY_MNEWBD_BTMRGN_MOD = "WO-DGMRM----A--";
    /**
     * Bottom roughness, rough.
     */
    String OCA_GPHY_MNEWBD_BTMRGN_RGH = "WO-DGMRR----A--";
    /**
     * Low.
     */
    String OCA_GPHY_MNEWBD_CTRB_LW = "WO-DGMCL----A--";
    /**
     * Medium.
     */
    String OCA_GPHY_MNEWBD_CTRB_MDM = "WO-DGMCM----A--";
    /**
     * High.
     */
    String OCA_GPHY_MNEWBD_CTRB_HGH = "WO-DGMCH----A--";
    /**
     * Impact burial, 0%.
     */
    String OCA_GPHY_MNEWBD_IMTBUR_0 = "WO-DGMIBA---A--";
    /**
     * Impact burial,  0-10%.
     */
    String OCA_GPHY_MNEWBD_IMTBUR_0_10 = "WO-DGMIBB---A--";
    /**
     * Impact burial,  10-20%.
     */
    String OCA_GPHY_MNEWBD_IMTBUR_10_20 = "WO-DGMIBC---A--";
    /**
     * Impact burial,  20-75%.
     */
    String OCA_GPHY_MNEWBD_IMTBUR_20_75 = "WO-DGMIBD---A--";
    /**
     * Impact burial, &gt;75%.
     */
    String OCA_GPHY_MNEWBD_IMTBUR_75 = "WO-DGMIBE---A--";
    /**
     * Miw bottom category, a.
     */
    String OCA_GPHY_MNEWBD_MIWBC_A = "WO-DGMBCA---A--";
    /**
     * Miw bottom category, b.
     */
    String OCA_GPHY_MNEWBD_MIWBC_B = "WO-DGMBCB---A--";
    /**
     * Miw bottom category, c.
     */
    String OCA_GPHY_MNEWBD_MIWBC_C = "WO-DGMBCC---A--";
    /**
     * Miw bottom type, a1.
     */
    String OCA_GPHY_MNEWBD_MIWBT_A1 = "WO-DGMBTA---A--";
    /**
     * Miw bottom type, a2.
     */
    String OCA_GPHY_MNEWBD_MIWBT_A2 = "WO-DGMBTB---A--";
    /**
     * Miw bottom type, a3.
     */
    String OCA_GPHY_MNEWBD_MIWBT_A3 = "WO-DGMBTC---A--";
    /**
     * Miw bottom type, b1.
     */
    String OCA_GPHY_MNEWBD_MIWBT_B1 = "WO-DGMBTD---A--";
    /**
     * Miw bottom type, b2.
     */
    String OCA_GPHY_MNEWBD_MIWBT_B2 = "WO-DGMBTE---A--";
    /**
     * Miw bottom type, b3.
     */
    String OCA_GPHY_MNEWBD_MIWBT_B3 = "WO-DGMBTF---A--";
    /**
     * Miw bottom type, c1.
     */
    String OCA_GPHY_MNEWBD_MIWBT_C1 = "WO-DGMBTG---A--";
    /**
     * Miw bottom type, c2.
     */
    String OCA_GPHY_MNEWBD_MIWBT_C2 = "WO-DGMBTH---A--";
    /**
     * Miw bottom type, c3.
     */
    String OCA_GPHY_MNEWBD_MIWBT_C3 = "WO-DGMBTI---A--";
    /**
     * Maritime limit boundary.
     */
    String OCA_LMT_MARTLB = "WO-DL-ML---L---";
    /**
     * Maritime area.
     */
    String OCA_LMT_MARTAR = "WO-DL-MA----A--";
    /**
     * Restricted area.
     */
    String OCA_LMT_RSDARA = "WO-DL-RA---L---";
    /**
     * Swept area.
     */
    String OCA_LMT_SWPARA = "WO-DL-SA----A--";
    /**
     * Training area.
     */
    String OCA_LMT_TRGARA = "WO-DL-TA----A--";
    /**
     * Operator-defined.
     */
    String OCA_LMT_OD = "WO-DL-O-----A--";
    /**
     * Submarine cable.
     */
    String OCA_MMD_SUBCBL = "WO-DMCA----L---";
    /**
     * Submerged crib.
     */
    String OCA_MMD_SBMCRB = "WO-DMCC-----A--";
    /**
     * Canal.
     */
    String OCA_MMD_CNL = "WO-DMCD----L---";
    /**
     * Ford.
     */
    String OCA_MMD_FRD = "WOS-MF----P----";
    /**
     * Lock.
     */
    String OCA_MMD_LCK = "WOS-ML----P----";
    /**
     * Oil/gas rig.
     */
    String OCA_MMD_OLRG = "WOS-MOA---P----";
    /**
     * Oil/gas rig field.
     */
    String OCA_MMD_OLRGFD = "WO-DMOA-----A--";
    /**
     * Pipelines/pipe.
     */
    String OCA_MMD_PPELNE = "WO-DMPA----L---";
    /**
     * Pile/piling/post.
     */
    String OCA_MMD_PLE = "WOS-MPA---P----";
}
