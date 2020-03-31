/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.symbology.milstd2525.graphics;

/**
 * SIDC constants for graphics in the "Tactical Graphics" scheme (MIL-STD-2525C Appendix B). The constants in this
 * interface are "masked" SIDCs. All fields except Scheme, Category, and Function ID are filled with hyphens. (The other
 * fields do not identity a type of graphic, they modify the graphic.)
 *
 * @author pabercrombie
 * @version $Id: TacGrpSidc.java 429 2012-03-04 23:35:53Z pabercrombie $
 */
public interface TacGrpSidc
{
    ///////////////////////////////
    // Tasks
    ///////////////////////////////

    /** Block */
    String TSK_BLK = "G-T-B----------";
    /** Breach */
    String TSK_BRH = "G-T-H----------";
    /** Bypass */
    String TSK_BYS = "G-T-Y----------";
    /** Canalize */
    String TSK_CNZ = "G-T-C----------";
    /** Clear */
    String TSK_CLR = "G-T-X----------";
    /** Contain */
    String TSK_CNT = "G-T-J----------";
    /** Counterattack (CATK) */
    String TSK_CATK = "G-T-K----------";
    /** Counterattack By Fire */
    String TSK_CATK_CATKF = "G-T-KF---------";
    /** Delay */
    String TSK_DLY = "G-T-L----------";
    /** Destroy */
    String TSK_DSTY = "G-T-D----------";
    /** Disrupt */
    String TSK_DRT = "G-T-T----------";
    /** Fix */
    String TSK_FIX = "G-T-F----------";
    /** Follow And Assume */
    String TSK_FLWASS = "G-T-A----------";
    /** Follow And Support */
    String TSK_FLWASS_FLWSUP = "G-T-AS---------";
    /** Interdict */
    String TSK_ITDT = "G-T-I----------";
    /** Isolate */
    String TSK_ISL = "G-T-E----------";
    /** Neutralize */
    String TSK_NEUT = "G-T-N----------";
    /** Occupy */
    String TSK_OCC = "G-T-O----------";
    /** Penetrate */
    String TSK_PNE = "G-T-P----------";
    /** Relief In Place (RIP) */
    String TSK_RIP = "G-T-R----------";
    /** Retain */
    String TSK_RTN = "G-T-Q----------";
    /** Retirement */
    String TSK_RTM = "G-T-M----------";
    /** Secure */
    String TSK_SCE = "G-T-S----------";
    /** Screen */
    String TSK_SEC_SCN = "G-T-US---------";
    /** Guard */
    String TSK_SEC_GUD = "G-T-UG---------";
    /** Cover */
    String TSK_SEC_COV = "G-T-UC---------";
    /** Seize */
    String TSK_SZE = "G-T-Z----------";
    /** Withdraw */
    String TSK_WDR = "G-T-W----------";
    /** Withdraw Under Pressure */
    String TSK_WDR_WDRUP = "G-T-WP---------";

    ///////////////////////////////////////////
    // Command, Control, and General Manuever
    ///////////////////////////////////////////

    /** Datum */
    String C2GM_GNL_PNT_USW_UH2_DTM = "G-G-GPUUD------";
    /** Brief Contact */
    String C2GM_GNL_PNT_USW_UH2_BCON = "G-G-GPUUB------";
    /** Lost Contact */
    String C2GM_GNL_PNT_USW_UH2_LCON = "G-G-GPUUL------";
    /** Sinker */
    String C2GM_GNL_PNT_USW_UH2_SNK = "G-G-GPUUS------";
    /** Sonobuoy */
    String C2GM_GNL_PNT_USW_SNBY = "G-G-GPUY-------";
    /** Pattern Center */
    String C2GM_GNL_PNT_USW_SNBY_PTNCTR = "G-G-GPUYP------";
    /** Directional Frequency Analyzing And Recording (DIFAR) */
    String C2GM_GNL_PNT_USW_SNBY_DIFAR = "G-G-GPUYD------";
    /** Low Frequency Analyzing And Recording (LOFAR) */
    String C2GM_GNL_PNT_USW_SNBY_LOFAR = "G-G-GPUYL------";
    /** Command Active Sonobuoy System (CASS) */
    String C2GM_GNL_PNT_USW_SNBY_CASS = "G-G-GPUYC------";
    /** Directional Command Active Sonobuoy System (DICASS) */
    String C2GM_GNL_PNT_USW_SNBY_DICASS = "G-G-GPUYS------";
    /** Bathythermograph Transmitting (BT) */
    String C2GM_GNL_PNT_USW_SNBY_BT = "G-G-GPUYB------";
    /** ANM */
    String C2GM_GNL_PNT_USW_SNBY_ANM = "G-G-GPUYA------";
    /** Vertical Line Array Difar (VLAD) */
    String C2GM_GNL_PNT_USW_SNBY_VLAD = "G-G-GPUYV------";
    /** ATAC */
    String C2GM_GNL_PNT_USW_SNBY_ATAC = "G-G-GPUYT------";
    /** Range Only (RO) */
    String C2GM_GNL_PNT_USW_SNBY_RO = "G-G-GPUYR------";
    /** Kingpin */
    String C2GM_GNL_PNT_USW_SNBY_KGP = "G-G-GPUYK------";
    /** Sonobuoy-Expired */
    String C2GM_GNL_PNT_USW_SNBY_EXP = "G-G-GPUYX------";
    /** Search */
    String C2GM_GNL_PNT_USW_SRH = "G-G-GPUS-------";
    /** Search Area */
    String C2GM_GNL_PNT_USW_SRH_ARA = "G-G-GPUSA------";
    /** DIP Position */
    String C2GM_GNL_PNT_USW_SRH_DIPPSN = "G-G-GPUSD------";
    /** Search Center */
    String C2GM_GNL_PNT_USW_SRH_CTR = "G-G-GPUSC------";
    /** Reference Point */
    String C2GM_GNL_PNT_REFPNT = "G-G-GPR--------";
    /** Navigational Reference Point */
    String C2GM_GNL_PNT_REFPNT_NAVREF = "G-G-GPRN-------";
    /** Special Point */
    String C2GM_GNL_PNT_REFPNT_SPLPNT = "G-G-GPRS-------";
    /** DLRP */
    String C2GM_GNL_PNT_REFPNT_DLRP = "G-G-GPRD-------";
    /** Point Of Intended Movement (PIM) */
    String C2GM_GNL_PNT_REFPNT_PIM = "G-G-GPRP-------";
    /** Marshall Point */
    String C2GM_GNL_PNT_REFPNT_MRSH = "G-G-GPRM-------";
    /** Waypoint */
    String C2GM_GNL_PNT_REFPNT_WAP = "G-G-GPRW-------";
    /** Corridor Tab */
    String C2GM_GNL_PNT_REFPNT_CRDRTB = "G-G-GPRC-------";
    /** Point Of Interest */
    String C2GM_GNL_PNT_REFPNT_PNTINR = "G-G-GPRI-------";
    /** Aim Point */
    String C2GM_GNL_PNT_WPN_AIMPNT = "G-G-GPWA-------";
    /** Drop Point */
    String C2GM_GNL_PNT_WPN_DRPPNT = "G-G-GPWD-------";
    /** Entry Point */
    String C2GM_GNL_PNT_WPN_ENTPNT = "G-G-GPWE-------";
    /** Ground Zero */
    String C2GM_GNL_PNT_WPN_GRDZRO = "G-G-GPWG-------";
    /** MSL Detect Point */
    String C2GM_GNL_PNT_WPN_MSLPNT = "G-G-GPWM-------";
    /** Impact Point */
    String C2GM_GNL_PNT_WPN_IMTPNT = "G-G-GPWI-------";
    /** Predicted Impact Point */
    String C2GM_GNL_PNT_WPN_PIPNT = "G-G-GPWP-------";
    /** Formation */
    String C2GM_GNL_PNT_FRMN = "G-G-GPF--------";
    /** Harbor (General) */
    String C2GM_GNL_PNT_HBR = "G-G-GPH--------";
    /** Point Q */
    String C2GM_GNL_PNT_HBR_PNTQ = "G-G-GPHQ-------";
    /** Point A */
    String C2GM_GNL_PNT_HBR_PNTA = "G-G-GPHA-------";
    /** Point Y */
    String C2GM_GNL_PNT_HBR_PNTY = "G-G-GPHY-------";
    /** Point X */
    String C2GM_GNL_PNT_HBR_PNTX = "G-G-GPHX-------";
    /** Route */
    String C2GM_GNL_PNT_RTE = "G-G-GPO--------";
    /** Rendezvous */
    String C2GM_GNL_PNT_RTE_RDV = "G-G-GPOZ-------";
    /** Diversions */
    String C2GM_GNL_PNT_RTE_DVSN = "G-G-GPOD-------";
    /** Waypoint */
    String C2GM_GNL_PNT_RTE_WAP = "G-G-GPOW-------";
    /** PIM */
    String C2GM_GNL_PNT_RTE_PIM = "G-G-GPOP-------";
    /** Point R */
    String C2GM_GNL_PNT_RTE_PNTR = "G-G-GPOR-------";
    /** Air Control */
    String C2GM_GNL_PNT_ACTL = "G-G-GPA--------";
    /** Combat Air Patrol (CAP) */
    String C2GM_GNL_PNT_ACTL_CAP = "G-G-GPAP-------";
    /** Airborne Early Warning (AEW) */
    String C2GM_GNL_PNT_ACTL_ABNEW = "G-G-GPAW-------";
    /** Tanking */
    String C2GM_GNL_PNT_ACTL_TAK = "G-G-GPAK-------";
    /** Antisubmarine Warfare, Fixed Wing */
    String C2GM_GNL_PNT_ACTL_ASBWF = "G-G-GPAA-------";
    /** Antisubmarine Warfare, Rotary Wing */
    String C2GM_GNL_PNT_ACTL_ASBWR = "G-G-GPAH-------";
    /** Sucap - Fixed Wing */
    String C2GM_GNL_PNT_ACTL_SUWF = "G-G-GPAB-------";
    /** Sucap - Rotary Wing */
    String C2GM_GNL_PNT_ACTL_SUWR = "G-G-GPAC-------";
    /** IW - Fixed Wing */
    String C2GM_GNL_PNT_ACTL_MIWF = "G-G-GPAD-------";
    /** MIW - Rotary Wing */
    String C2GM_GNL_PNT_ACTL_MIWR = "G-G-GPAE-------";
    /** Strike Ip */
    String C2GM_GNL_PNT_ACTL_SKEIP = "G-G-GPAS-------";
    /** Tacan */
    String C2GM_GNL_PNT_ACTL_TCN = "G-G-GPAT-------";
    /** Tomcat */
    String C2GM_GNL_PNT_ACTL_TMC = "G-G-GPAO-------";
    /** Rescue */
    String C2GM_GNL_PNT_ACTL_RSC = "G-G-GPAR-------";
    /** Replenish */
    String C2GM_GNL_PNT_ACTL_RPH = "G-G-GPAL-------";
    /** Unmanned Aerial System (UAS/UA) */
    String C2GM_GNL_PNT_ACTL_UA = "G-G-GPAF-------";
    /** VTUA */
    String C2GM_GNL_PNT_ACTL_VTUA = "G-G-GPAG-------";
    /** Orbit */
    String C2GM_GNL_PNT_ACTL_ORB = "G-G-GPAI-------";
    /** Orbit - Figure Eight */
    String C2GM_GNL_PNT_ACTL_ORBF8 = "G-G-GPAJ-------";
    /** Orbit - Race Track */
    String C2GM_GNL_PNT_ACTL_ORBRT = "G-G-GPAM-------";
    /** Orbit - Random, Closed */
    String C2GM_GNL_PNT_ACTL_ORBRD = "G-G-GPAN-------";
    /** Action Points (General) */
    String C2GM_GNL_PNT_ACTPNT = "G-G-GPP--------";
    /** Check Point */
    String C2GM_GNL_PNT_ACTPNT_CHKPNT = "G-G-GPPK-------";
    /** Contact Point */
    String C2GM_GNL_PNT_ACTPNT_CONPNT = "G-G-GPPC-------";
    /** Coordination Point */
    String C2GM_GNL_PNT_ACTPNT_CRDPNT = "G-G-GPPO-------";
    /** Decision Point */
    String C2GM_GNL_PNT_ACTPNT_DCNPNT = "G-G-GPPD-------";
    /** Linkup Point */
    String C2GM_GNL_PNT_ACTPNT_LNKUPT = "G-G-GPPL-------";
    /** Passage Point */
    String C2GM_GNL_PNT_ACTPNT_PSSPNT = "G-G-GPPP-------";
    /** Rally Point */
    String C2GM_GNL_PNT_ACTPNT_RAYPNT = "G-G-GPPR-------";
    /** Release Point */
    String C2GM_GNL_PNT_ACTPNT_RELPNT = "G-G-GPPE-------";
    /** Start Point */
    String C2GM_GNL_PNT_ACTPNT_STRPNT = "G-G-GPPS-------";
    /** Amnesty Point */
    String C2GM_GNL_PNT_ACTPNT_AMNPNT = "G-G-GPPA-------";
    /** Waypoint */
    String C2GM_GNL_PNT_ACTPNT_WAP = "G-G-GPPW-------";
    /** EA Surface Control Station */
    String C2GM_GNL_PNT_SCTL = "G-G-GPC--------";
    /** Unmanned Surface Vehicle (USV) Control Station */
    String C2GM_GNL_PNT_SCTL_USV = "G-G-GPCU-------";
    /** Remote Multimission Vehicle (RMV) Usv Control Station */
    String C2GM_GNL_PNT_SCTL_USV_RMV = "G-G-GPCUR------";
    /** USV - Antisubmarine Warfare Control Station */
    String C2GM_GNL_PNT_SCTL_USV_ASW = "G-G-GPCUA------";
    /** USV - Surface Warfare Control Station */
    String C2GM_GNL_PNT_SCTL_USV_SUW = "G-G-GPCUS------";
    /** USV - Mine Warfare Control Station */
    String C2GM_GNL_PNT_SCTL_USV_MIW = "G-G-GPCUM------";
    /** ASW Control Station */
    String C2GM_GNL_PNT_SCTL_ASW = "G-G-GPCA-------";
    /** SUW Control Station */
    String C2GM_GNL_PNT_SCTL_SUW = "G-G-GPCS-------";
    /** MIW Control Station */
    String C2GM_GNL_PNT_SCTL_MIW = "G-G-GPCM-------";
    /** Picket Control Station */
    String C2GM_GNL_PNT_SCTL_PKT = "G-G-GPCP-------";
    /** Rendezvous Control Point */
    String C2GM_GNL_PNT_SCTL_RDV = "G-G-GPCR-------";
    /** Rescue Control Point */
    String C2GM_GNL_PNT_SCTL_RSC = "G-G-GPCC-------";
    /** Replenishment Control Point */
    String C2GM_GNL_PNT_SCTL_REP = "G-G-GPCE-------";
    /** Noncombatant Control Station */
    String C2GM_GNL_PNT_SCTL_NCBTT = "G-G-GPCN-------";
    /** Subsurface Control Station */
    String C2GM_GNL_PNT_UCTL = "G-G-GPB--------";
    /** Unmanned Underwater Vehicle (UUV) Control Station */
    String C2GM_GNL_PNT_UCTL_UUV = "G-G-GPBU-------";
    /** UUV - Antisubmarine Warfare Control Station */
    String C2GM_GNL_PNT_UCTL_UUV_ASW = "G-G-GPBUA------";
    /** UUV - Surface Warfare Control Station */
    String C2GM_GNL_PNT_UCTL_UUV_SUW = "G-G-GPBUS------";
    /** UUV - Mine Warfare Control Station */
    String C2GM_GNL_PNT_UCTL_UUV_MIW = "G-G-GPBUM------";
    /** Submarine Control Station */
    String C2GM_GNL_PNT_UCTL_SBSTN = "G-G-GPBS-------";
    /** ASW Submarine Control Station */
    String C2GM_GNL_PNT_UCTL_SBSTN_ASW = "G-G-GPBSA------";
    /** Boundaries */
    String C2GM_GNL_LNE_BNDS = "G-G-GLB--------";
    /** Forward Line of Own Troops */
    String C2GM_GNL_LNE_FLOT = "G-G-GLF--------";
    /** Line Of Contact */
    String C2GM_GNL_LNE_LOC = "G-G-GLC--------";
    /** Phase Line */
    String C2GM_GNL_LNE_PHELNE = "G-G-GLP--------";
    /** Light Line */
    String C2GM_GNL_LNE_LITLNE = "G-G-GLL--------";
    /** Areas */
    String C2GM_GNL_ARS = "G-G-GA---------";
    /** General Area */
    String C2GM_GNL_ARS_GENARA = "G-G-GAG--------";
    /** Assembly Area */
    String C2GM_GNL_ARS_ABYARA = "G-G-GAA--------";
    /** Engagement Area */
    String C2GM_GNL_ARS_EMTARA = "G-G-GAE--------";
    /** Fortified Area */
    String C2GM_GNL_ARS_FTFDAR = "G-G-GAF--------";
    /** Drop Zone */
    String C2GM_GNL_ARS_DRPZ = "G-G-GAD--------";
    /** Extraction Zone (EZ) */
    String C2GM_GNL_ARS_EZ = "G-G-GAX--------";
    /** Landing Zone (LZ) */
    String C2GM_GNL_ARS_LZ = "G-G-GAL--------";
    /** Pickup Zone (PZ) */
    String C2GM_GNL_ARS_PZ = "G-G-GAP--------";
    /** Search Area/Reconnaissance Area */
    String C2GM_GNL_ARS_SRHARA = "G-G-GAS--------";
    /** Limited Access Area */
    String C2GM_GNL_ARS_LAARA = "G-G-GAY--------";
    /** Airfield Zone */
    String C2GM_GNL_ARS_AIRFZ = "G-G-GAZ--------";
    /** Air Control Point (ACP) */
    String C2GM_AVN_PNT_ACP = "G-G-APP--------";
    /** Communications Checkpoint (CCP) */
    String C2GM_AVN_PNT_COMMCP = "G-G-APC--------";
    /** Pull-Up Point (PUP) */
    String C2GM_AVN_PNT_PUP = "G-G-APU--------";
    /** Downed Aircrew Pickup Point */
    String C2GM_AVN_PNT_DAPP = "G-G-APD--------";
    /** Air Corridor */
    String C2GM_AVN_LNE_ACDR = "G-G-ALC--------";
    /** Minimum Risk Route (MRR) */
    String C2GM_AVN_LNE_MRR = "G-G-ALM--------";
    /** Standard-Use Army Aircraft Flight Route (SAAFR) */
    String C2GM_AVN_LNE_SAAFR = "G-G-ALS--------";
    /** Unmanned Aircraft (UA) Route */
    String C2GM_AVN_LNE_UAR = "G-G-ALU--------";
    /** Low Level Transit Route (LLTR) */
    String C2GM_AVN_LNE_LLTR = "G-G-ALL--------";
    /** Restricted Operations Zone (ROZ) */
    String C2GM_AVN_ARS_ROZ = "G-G-AAR--------";
    /** Short-Range Air Defense Engagement Zone (SHORADEZ) */
    String C2GM_AVN_ARS_SHRDEZ = "G-G-AAF--------";
    /** High Density Airspace Control Zone (HIDACZ) */
    String C2GM_AVN_ARS_HIDACZ = "G-G-AAH--------";
    /** Missile Engagement Zone (MEZ) */
    String C2GM_AVN_ARS_MEZ = "G-G-AAM--------";
    /** Low Altitude Mez */
    String C2GM_AVN_ARS_MEZ_LAMEZ = "G-G-AAML-------";
    /** High Altitude Mez */
    String C2GM_AVN_ARS_MEZ_HAMEZ = "G-G-AAMH-------";
    /** Weapons Free Zone */
    String C2GM_AVN_ARS_WFZ = "G-G-AAW--------";
    /** Dummy (Deception/Decoy) */
    String C2GM_DCPN_DMY = "G-G-PD---------";
    /** Axis  Of Advance For Feint */
    String C2GM_DCPN_AAFF = "G-G-PA---------";
    /** Direction Of Attack For Feint */
    String C2GM_DCPN_DAFF = "G-G-PF---------";
    /** Decoy Mined Area */
    String C2GM_DCPN_DMA = "G-G-PM---------";
    /** Decoy Mined Area,  Fenced */
    String C2GM_DCPN_DMAF = "G-G-PY---------";
    /** Dummy Minefield (Static) */
    String C2GM_DCPN_DMYMS = "G-G-PN---------";
    /** Dummy Minefield (Dynamic) */
    String C2GM_DCPN_DMYMD = "G-G-PC---------";
    /** Target Reference Point (TRP) */
    String C2GM_DEF_PNT_TGTREF = "G-G-DPT--------";
    /** Observation Post/Outpost */
    String C2GM_DEF_PNT_OBSPST = "G-G-DPO--------";
    /** Combat  Outpost */
    String C2GM_DEF_PNT_OBSPST_CBTPST = "G-G-DPOC-------";
    /** Observation Post Occupied By Dismounted Scouts Or Reconnaissance */
    String C2GM_DEF_PNT_OBSPST_RECON = "G-G-DPOR-------";
    /** Forward Observer Position */
    String C2GM_DEF_PNT_OBSPST_FWDOP = "G-G-DPOF-------";
    /** Sensor Outpost/Listening Post (OP/Lp) */
    String C2GM_DEF_PNT_OBSPST_SOP = "G-G-DPOS-------";
    /** Cbrn Observation Post (Dismounted) */
    String C2GM_DEF_PNT_OBSPST_CBRNOP = "G-G-DPON-------";
    /** Forward Edge Of Battle Area (FEBA) */
    String C2GM_DEF_LNE_FEBA = "G-G-DLF--------";
    /** Principal Direction Of Fire (PDF) */
    String C2GM_DEF_LNE_PDF = "G-G-DLP--------";
    /** Battle Position */
    String C2GM_DEF_ARS_BTLPSN = "G-G-DAB--------";
    /** Prepared But Not Occupied */
    String C2GM_DEF_ARS_BTLPSN_PBNO = "G-G-DABP-------";
    /** Engagement Area */
    String C2GM_DEF_ARS_EMTARA = "G-G-DAE--------";
    /** Point Of Departure */
    String C2GM_OFF_PNT_PNTD = "G-G-OPP--------";
    /** Axis Of Advance */
    String C2GM_OFF_LNE_AXSADV = "G-G-OLA--------";
    /** Aviation */
    String C2GM_OFF_LNE_AXSADV_AVN = "G-G-OLAV-------";
    /** Airborne */
    String C2GM_OFF_LNE_AXSADV_ABN = "G-G-OLAA-------";
    /** Attack, Rotary Wing */
    String C2GM_OFF_LNE_AXSADV_ATK = "G-G-OLAR-------";
    /** Ground */
    String C2GM_OFF_LNE_AXSADV_GRD = "G-G-OLAG-------";
    /** Main Attack */
    String C2GM_OFF_LNE_AXSADV_GRD_MANATK = "G-G-OLAGM------";
    /** Supporting Attack */
    String C2GM_OFF_LNE_AXSADV_GRD_SUPATK = "G-G-OLAGS------";
    /** Aviation */
    String C2GM_OFF_LNE_DIRATK_AVN = "G-G-OLKA-------";
    /** Main Ground Attack */
    String C2GM_OFF_LNE_DIRATK_GRD_MANATK = "G-G-OLKGM------";
    /** Supporting Ground Attack */
    String C2GM_OFF_LNE_DIRATK_GRD_SUPATK = "G-G-OLKGS------";
    /** Final Coordination Line */
    String C2GM_OFF_LNE_FCL = "G-G-OLF--------";
    /** Infiltration Lane */
    String C2GM_OFF_LNE_INFNLE = "G-G-OLI--------";
    /** Limit Of Advance */
    String C2GM_OFF_LNE_LMTADV = "G-G-OLL--------";
    /** Line Of Departure */
    String C2GM_OFF_LNE_LD = "G-G-OLT--------";
    /** Line Of Departure/Line Of Contact (LD/LC) */
    String C2GM_OFF_LNE_LDLC = "G-G-OLC--------";
    /** Probable Line Of Deployment (PLD) */
    String C2GM_OFF_LNE_PLD = "G-G-OLP--------";
    /** Assault Position */
    String C2GM_OFF_ARS_ASTPSN = "G-G-OAA--------";
    /** Attack Position */
    String C2GM_OFF_ARS_ATKPSN = "G-G-OAK--------";
    /** Attack By Fire Position */
    String C2GM_OFF_ARS_AFP = "G-G-OAF--------";
    /** Support By Fire Position */
    String C2GM_OFF_ARS_SFP = "G-G-OAS--------";
    /** Objective */
    String C2GM_OFF_ARS_OBJ = "G-G-OAO--------";
    /** Penetration Box */
    String C2GM_OFF_ARS_PBX = "G-G-OAP--------";
    /** Ambush */
    String C2GM_SPL_LNE_AMB = "G-G-SLA--------";
    /** Holding Line */
    String C2GM_SPL_LNE_HGL = "G-G-SLH--------";
    /** Release Line */
    String C2GM_SPL_LNE_REL = "G-G-SLR--------";
    /** Bridgehead */
    String C2GM_SPL_LNE_BRGH = "G-G-SLB--------";
    /** Area */
    String C2GM_SPL_ARA = "G-G-SA---------";
    /** Area Of Operations (AO) */
    String C2GM_SPL_ARA_AOO = "G-G-SAO--------";
    /** Airhead */
    String C2GM_SPL_ARA_AHD = "G-G-SAA--------";
    /** Encirclement */
    String C2GM_SPL_ARA_ENCMT = "G-G-SAE--------";
    /** Named */
    String C2GM_SPL_ARA_NAI = "G-G-SAN--------";
    /** Targeted Area Of Interest (TAI) */
    String C2GM_SPL_ARA_TAI = "G-G-SAT--------";

    ///////////////////////////////////////////
    // Mobility/Survivability
    ///////////////////////////////////////////

    /** Belt */
    String MOBSU_OBST_GNL_BLT = "G-M-OGB--------";
    /** Line */
    String MOBSU_OBST_GNL_LNE = "G-M-OGL--------";
    /** Zone */
    String MOBSU_OBST_GNL_Z = "G-M-OGZ--------";
    /** Obstacle Free Area */
    String MOBSU_OBST_GNL_OFA = "G-M-OGF--------";
    /** Obstacle Restricted Area */
    String MOBSU_OBST_GNL_ORA = "G-M-OGR--------";
    /** Abatis */
    String MOBSU_OBST_ABS = "G-M-OS---------";
    /** Antitank Ditch, Under Construction */
    String MOBSU_OBST_ATO_ATD_ATDUC = "G-M-OADU-------";
    /** Antitank Ditch, Complete */
    String MOBSU_OBST_ATO_ATD_ATDC = "G-M-OADC-------";
    /** Antitank Ditch Reinforced With Antitank Mines */
    String MOBSU_OBST_ATO_ATDATM = "G-M-OAR--------";
    /** Fixed And Prefabricated */
    String MOBSU_OBST_ATO_TDTSM_FIXPFD = "G-M-OAOF-------";
    /** Moveable */
    String MOBSU_OBST_ATO_TDTSM_MVB = "G-M-OAOM-------";
    /** Moveable And Prefabricated */
    String MOBSU_OBST_ATO_TDTSM_MVBPFD = "G-M-OAOP-------";
    /** Antitank Wall */
    String MOBSU_OBST_ATO_ATW = "G-M-OAW--------";
    /** Booby Trap */
    String MOBSU_OBST_BBY = "G-M-OB---------";
    /** Unspecified Mine */
    String MOBSU_OBST_MNE_USPMNE = "G-M-OMU--------";
    /** Antitank Mine (AT) */
    String MOBSU_OBST_MNE_ATMNE = "G-M-OMT--------";
    /** Antitank Mine With Antihandling Device */
    String MOBSU_OBST_MNE_ATMAHD = "G-M-OMD--------";
    /** Antitank Mine (Directional) */
    String MOBSU_OBST_MNE_ATMDIR = "G-M-OME--------";
    /** Antipersonnel (AP) Mines */
    String MOBSU_OBST_MNE_APMNE = "G-M-OMP--------";
    /** Wide Area Mines */
    String MOBSU_OBST_MNE_WAMNE = "G-M-OMW--------";
    /** Mine Cluster */
    String MOBSU_OBST_MNE_MCLST = "G-M-OMC--------";
    /** Static Depiction */
    String MOBSU_OBST_MNEFLD_STC = "G-M-OFS--------";
    /** Dynamic Depiction */
    String MOBSU_OBST_MNEFLD_DYN = "G-M-OFD--------";
    /** Gap */
    String MOBSU_OBST_MNEFLD_GAP = "G-M-OFG--------";
    /** Mined Area */
    String MOBSU_OBST_MNEFLD_MNDARA = "G-M-OFA--------";
    /** Block */
    String MOBSU_OBST_OBSEFT_BLK = "G-M-OEB--------";
    /** Fix */
    String MOBSU_OBST_OBSEFT_FIX = "G-M-OEF--------";
    /** Turn */
    String MOBSU_OBST_OBSEFT_TUR = "G-M-OET--------";
    /** Disrupt */
    String MOBSU_OBST_OBSEFT_DRT = "G-M-OED--------";
    /** Unexploded Ordnance Area (UXO) */
    String MOBSU_OBST_UXO = "G-M-OU---------";
    /** Planned */
    String MOBSU_OBST_RCBB_PLND = "G-M-ORP--------";
    /** Explosives, State Of Readiness 1 (Safe) */
    String MOBSU_OBST_RCBB_SAFE = "G-M-ORS--------";
    /** Explosives, State Of Readiness 2 (Armed-But Passable) */
    String MOBSU_OBST_RCBB_ABP = "G-M-ORA--------";
    /** Roadblock Complete (Executed) */
    String MOBSU_OBST_RCBB_EXCD = "G-M-ORC--------";
    /** Trip Wire */
    String MOBSU_OBST_TRIPWR = "G-M-OT---------";
    /** Wire Obstacle */
    String MOBSU_OBST_WREOBS = "G-M-OW---------";
    /** Unspecified */
    String MOBSU_OBST_WREOBS_USP = "G-M-OWU--------";
    /** Single Fence */
    String MOBSU_OBST_WREOBS_SNGFNC = "G-M-OWS--------";
    /** Double Fence */
    String MOBSU_OBST_WREOBS_DBLFNC = "G-M-OWD--------";
    /** Double Apron Fence */
    String MOBSU_OBST_WREOBS_DAFNC = "G-M-OWA--------";
    /** Low Wire Fence */
    String MOBSU_OBST_WREOBS_LWFNC = "G-M-OWL--------";
    /** High Wire Fence */
    String MOBSU_OBST_WREOBS_HWFNC = "G-M-OWH--------";
    /** Single Concertina */
    String MOBSU_OBST_WREOBS_CCTA_SNG = "G-M-OWCS-------";
    /** Double Strand Concertina */
    String MOBSU_OBST_WREOBS_CCTA_DBLSTD = "G-M-OWCD-------";
    /** Triple Strand Concertina */
    String MOBSU_OBST_WREOBS_CCTA_TRISTD = "G-M-OWCT-------";
    /** Low Tower */
    String MOBSU_OBST_AVN_TWR_LOW = "G-M-OHTL-------";
    /** High Tower */
    String MOBSU_OBST_AVN_TWR_HIGH = "G-M-OHTH-------";
    /** Overhead Wire/Power Line */
    String MOBSU_OBST_AVN_OHWIRE = "G-M-OHO--------";
    /** Bypass Easy */
    String MOBSU_OBSTBP_DFTY_ESY = "G-M-BDE--------";
    /** Bypass Difficult */
    String MOBSU_OBSTBP_DFTY_DFT = "G-M-BDD--------";
    /** Bypass Impossible */
    String MOBSU_OBSTBP_DFTY_IMP = "G-M-BDI--------";
    /** Crossing Site/Water Crossing */
    String MOBSU_OBSTBP_CSGSTE = "G-M-BC---------";
    /** Assault Crossing Area */
    String MOBSU_OBSTBP_CSGSTE_ASTCA = "G-M-BCA--------";
    /** Bridge or Gap */
    String MOBSU_OBSTBP_CSGSTE_BRG = "G-M-BCB--------";
    /** Ferry */
    String MOBSU_OBSTBP_CSGSTE_FRY = "G-M-BCF--------";
    /** Ford Easy */
    String MOBSU_OBSTBP_CSGSTE_FRDESY = "G-M-BCE--------";
    /** Ford Difficult */
    String MOBSU_OBSTBP_CSGSTE_FRDDFT = "G-M-BCD--------";
    /** Lane */
    String MOBSU_OBSTBP_CSGSTE_LANE = "G-M-BCL--------";
    /** Raft Site */
    String MOBSU_OBSTBP_CSGSTE_RFT = "G-M-BCR--------";
    /** Engineer Regulating Point */
    String MOBSU_OBSTBP_CSGSTE_ERP = "G-M-BCP--------";
    /** Earthwork, Small Trench Or Fortification */
    String MOBSU_SU_ESTOF = "G-M-SE---------";
    /** Fort */
    String MOBSU_SU_FRT = "G-M-SF---------";
    /** Fortified Line */
    String MOBSU_SU_FTFDLN = "G-M-SL---------";
    /** Foxhole, Emplacement Or Weapon Site */
    String MOBSU_SU_FEWS = "G-M-SW---------";
    /** Strong Point */
    String MOBSU_SU_STRGPT = "G-M-SP---------";
    /** Surface Shelter */
    String MOBSU_SU_SUFSHL = "G-M-SS---------";
    /** Underground Shelter */
    String MOBSU_SU_UGDSHL = "G-M-SU---------";
    /** Minimum Safe Distance Zones */
    String MOBSU_CBRN_MSDZ = "G-M-NM---------";
    /** Nuclear Detonations Ground Zero */
    String MOBSU_CBRN_NDGZ = "G-M-NZ---------";
    /** Fallout Producing */
    String MOBSU_CBRN_FAOTP = "G-M-NF---------";
    /** Radioactive Area */
    String MOBSU_CBRN_RADA = "G-M-NR---------";
    /** Biologically Contaminated Area */
    String MOBSU_CBRN_BIOCA = "G-M-NB---------";
    /** Chemically Contaminated Area */
    String MOBSU_CBRN_CMLCA = "G-M-NC---------";
    /** Biological Release Event */
    String MOBSU_CBRN_REEVNT_BIO = "G-M-NEB--------";
    /** Chemical Release Event */
    String MOBSU_CBRN_REEVNT_CML = "G-M-NEC--------";
    /** Decon Site/Point (Unspecified) */
    String MOBSU_CBRN_DECONP_USP = "G-M-NDP--------";
    /** Alternate Decon Site/Point (Unspecified) */
    String MOBSU_CBRN_DECONP_ALTUSP = "G-M-NDA--------";
    /** Decon Site/Point (Troops) */
    String MOBSU_CBRN_DECONP_TRP = "G-M-NDT--------";
    /** Decon , */
    String MOBSU_CBRN_DECONP_EQT = "G-M-NDE--------";
    /** Decon Site/Point (Equipment And Troops) */
    String MOBSU_CBRN_DECONP_EQTTRP = "G-M-NDB--------";
    /** Decon Site/Point (Operational Decontamination) */
    String MOBSU_CBRN_DECONP_OPDECN = "G-M-NDO--------";
    /** Decon Site/Point (Thorough Decontamination) */
    String MOBSU_CBRN_DECONP_TRGH = "G-M-NDD--------";
    /** Dose Rate Contour Lines */
    String MOBSU_CBRN_DRCL = "G-M-NL---------";

    /////////////////////////////////////////////////
    // Fire Support
    /////////////////////////////////////////////////

    /** Point/Single Target */
    String FSUPP_PNT_TGT_PTGT = "G-F-PTS--------";
    /** Nuclear Target */
    String FSUPP_PNT_TGT_NUCTGT = "G-F-PTN--------";
    /** Fire Support Station */
    String FSUPP_PNT_C2PNT_FSS = "G-F-PCF--------";
    /** Survey Control Point */
    String FSUPP_PNT_C2PNT_SCP = "G-F-PCS--------";
    /** Firing Point */
    String FSUPP_PNT_C2PNT_FP = "G-F-PCB--------";
    /** Reload Point */
    String FSUPP_PNT_C2PNT_RP = "G-F-PCR--------";
    /** Hide Point */
    String FSUPP_PNT_C2PNT_HP = "G-F-PCH--------";
    /** Launch Point */
    String FSUPP_PNT_C2PNT_LP = "G-F-PCL--------";
    /** Linear Target */
    String FSUPP_LNE_LNRTGT = "G-F-LT---------";
    /** Linear Smoke Target */
    String FSUPP_LNE_LNRTGT_LSTGT = "G-F-LTS--------";
    /** Final Protective Fire (FPF) */
    String FSUPP_LNE_LNRTGT_FPF = "G-F-LTF--------";
    /** Fire Support Coordination Line (FSCL) */
    String FSUPP_LNE_C2LNE_FSCL = "G-F-LCF--------";
    /** Coordinated Fire Line (CFL) */
    String FSUPP_LNE_C2LNE_CFL = "G-F-LCC--------";
    /** No-Fire Line (NFL) */
    String FSUPP_LNE_C2LNE_NFL = "G-F-LCN--------";
    /** Restrictive */
    String FSUPP_LNE_C2LNE_RFL = "G-F-LCR--------";
    /** Munition Flight Path (MFP) */
    String FSUPP_LNE_C2LNE_MFP = "G-F-LCM--------";
    /** Area Target */
    String FSUPP_ARS_ARATGT = "G-F-AT---------";
    /** Rectangular Target */
    String FSUPP_ARS_ARATGT_RTGTGT = "G-F-ATR--------";
    /** Circular Target */
    String FSUPP_ARS_ARATGT_CIRTGT = "G-F-ATC--------";
    /** Series Or Group Of Targets */
    String FSUPP_ARS_ARATGT_SGTGT = "G-F-ATG--------";
    /** Smoke */
    String FSUPP_ARS_ARATGT_SMK = "G-F-ATS--------";
    /** Bomb Area */
    String FSUPP_ARS_ARATGT_BMARA = "G-F-ATB--------";
    /** Fire Support Area (FSA), Irregular */
    String FSUPP_ARS_C2ARS_FSA_IRR = "G-F-ACSI-------";
    /** Fire Support Area (FSA), Rectangular */
    String FSUPP_ARS_C2ARS_FSA_RTG = "G-F-ACSR-------";
    /** Fire Support Area (FSA), Circular */
    String FSUPP_ARS_C2ARS_FSA_CIRCLR = "G-F-ACSC-------";
    /** Airspace Coordination Area (ACA), Irregular */
    String FSUPP_ARS_C2ARS_ACA_IRR = "G-F-ACAI-------";
    /** Airspace Coordination Area (ACA), Rectangular */
    String FSUPP_ARS_C2ARS_ACA_RTG = "G-F-ACAR-------";
    /** Airspace Coordination Area (ACA), Circular */
    String FSUPP_ARS_C2ARS_ACA_CIRCLR = "G-F-ACAC-------";
    /** Free Fire Area (FFA), Irregular */
    String FSUPP_ARS_C2ARS_FFA_IRR = "G-F-ACFI-------";
    /** Free Fire Area (FFA), Rectangular */
    String FSUPP_ARS_C2ARS_FFA_RTG = "G-F-ACFR-------";
    /** Free Fire Area (FFA), Circular */
    String FSUPP_ARS_C2ARS_FFA_CIRCLR = "G-F-ACFC-------";
    /** No Fire Area (NFA), Irregular */
    String FSUPP_ARS_C2ARS_NFA_IRR = "G-F-ACNI-------";
    /** No Fire Area (NFA), Rectangular */
    String FSUPP_ARS_C2ARS_NFA_RTG = "G-F-ACNR-------";
    /** No , Circular */
    String FSUPP_ARS_C2ARS_NFA_CIRCLR = "G-F-ACNC-------";
    /** Restrictive Fire Area (RFA), Irregular */
    String FSUPP_ARS_C2ARS_RFA_IRR = "G-F-ACRI-------";
    /** Restrictive Fire Area (RFA), Rectangular */
    String FSUPP_ARS_C2ARS_RFA_RTG = "G-F-ACRR-------";
    /** Restrictive Fire Area (RFA), Circular */
    String FSUPP_ARS_C2ARS_RFA_CIRCLR = "G-F-ACRC-------";
    /** Position Area For Artillery (PAA), Rectangular */
    String FSUPP_ARS_C2ARS_PAA_RTG = "G-F-ACPR-------";
    /** Position Area For Artillery (PAA), Circular */
    String FSUPP_ARS_C2ARS_PAA_CIRCLR = "G-F-ACPC-------";
    /** Sensor Zone, Irregular */
    String FSUPP_ARS_C2ARS_SNSZ_IRR = "G-F-ACEI-------";
    /** Sensor Zone, Rectangular */
    String FSUPP_ARS_C2ARS_SNSZ_RTG = "G-F-ACER-------";
    /** Sensor Zone ,  Circular */
    String FSUPP_ARS_C2ARS_SNSZ_CIRCLR = "G-F-ACEC-------";
    /** Dead Space Area (DA),  Irregular */
    String FSUPP_ARS_C2ARS_DA_IRR = "G-F-ACDI-------";
    /** Dead Space Area (DA),  Rectangular */
    String FSUPP_ARS_C2ARS_DA_RTG = "G-F-ACDR-------";
    /** Dead Space Area (DA),  Circular */
    String FSUPP_ARS_C2ARS_DA_CIRCLR = "G-F-ACDC-------";
    /** Zone Of Responsibility (ZOR), Irregular */
    String FSUPP_ARS_C2ARS_ZOR_IRR = "G-F-ACZI-------";
    /** Zone Of Responsibility (ZOR), Rectangular */
    String FSUPP_ARS_C2ARS_ZOR_RTG = "G-F-ACZR-------";
    /** Zone Of Responsibility (ZOR), Circular */
    String FSUPP_ARS_C2ARS_ZOR_CIRCLR = "G-F-ACZC-------";
    /** Target Build Up Area (TBA), Irregular */
    String FSUPP_ARS_C2ARS_TBA_IRR = "G-F-ACBI-------";
    /** Target Build Up Area (TBA),Rectangular */
    String FSUPP_ARS_C2ARS_TBA_RTG = "G-F-ACBR-------";
    /** Target Build Up Area (TBA), Circular */
    String FSUPP_ARS_C2ARS_TBA_CIRCLR = "G-F-ACBC-------";
    /** Target , Irregular */
    String FSUPP_ARS_C2ARS_TVAR_IRR = "G-F-ACVI-------";
    /** Target Value Area (TVAR), Rectangular */
    String FSUPP_ARS_C2ARS_TVAR_RTG = "G-F-ACVR-------";
    /** Target Value Area (TVAR), Circular */
    String FSUPP_ARS_C2ARS_TVAR_CIRCLR = "G-F-ACVC-------";
    /** Terminally Guided Munition Footprint (TGMF) */
    String FSUPP_ARS_C2ARS_TGMF = "G-F-ACT--------";
    /** Artillery Target Intelligence (ATI) Zone, Irregular */
    String FSUPP_ARS_TGTAQZ_ATIZ_IRR = "G-F-AZII-------";
    /** Artillery Target Intelligence (ATI) Zone, Rectangular */
    String FSUPP_ARS_TGTAQZ_ATIZ_RTG = "G-F-AZIR-------";
    /** Call For Fire Zone (CFFZ), Irregular */
    String FSUPP_ARS_TGTAQZ_CFFZ_IRR = "G-F-AZXI-------";
    /** Call For Fire Zone (CFFZ), Rectangular */
    String FSUPP_ARS_TGTAQZ_CFFZ_RTG = "G-F-AZXR-------";
    /** Censor Zone,  Irregular */
    String FSUPP_ARS_TGTAQZ_CNS_IRR = "G-F-AZCI-------";
    /** Censor Zone, Rectangular */
    String FSUPP_ARS_TGTAQZ_CNS_RTG = "G-F-AZCR-------";
    /** Critical Friendly Zone (CFZ), Irregular */
    String FSUPP_ARS_TGTAQZ_CFZ_IRR = "G-F-AZFI-------";
    /** Critical Friendly Zone (CFZ), Rectangular */
    String FSUPP_ARS_TGTAQZ_CFZ_RTG = "G-F-AZFR-------";
    /** Weapon/Sensor Range Fan, Circular */
    String FSUPP_ARS_WPNRF_CIRCLR = "G-F-AXC--------";
    /** Weapon/Sensor Range Fan, Sector */
    String FSUPP_ARS_WPNRF_SCR = "G-F-AXS--------";
    /** Blue Kill Box,  Circular */
    String FSUPP_ARS_KLBOX_BLUE_CIRCLR = "G-F-AKBC-------";
    /** Blue Kill Box, Irregular */
    String FSUPP_ARS_KLBOX_BLUE_IRR = "G-F-AKBI-------";
    /** Blue , Rectangular */
    String FSUPP_ARS_KLBOX_BLUE_RTG = "G-F-AKBR-------";
    /** Purple Kill Box, Circular */
    String FSUPP_ARS_KLBOX_PURPLE_CIRCLR = "G-F-AKPC-------";
    /** Purple Kill Box, Irregular */
    String FSUPP_ARS_KLBOX_PURPLE_IRR = "G-F-AKPI-------";
    /** Purple Kill Box, Rectangular */
    String FSUPP_ARS_KLBOX_PURPLE_RTG = "G-F-AKPR-------";

    ////////////////////////////////////////////////
    // Combat Service Support
    ////////////////////////////////////////////////

    /** Ambulance Exchange Point */
    String CSS_PNT_AEP = "G-S-PX---------";
    /** Cannibalization Point */
    String CSS_PNT_CBNP = "G-S-PC---------";
    /** Casualty Collection Point */
    String CSS_PNT_CCP = "G-S-PY---------";
    /** Civilian Collection Point */
    String CSS_PNT_CVP = "G-S-PT---------";
    /** Detainee Collection Point */
    String CSS_PNT_DCP = "G-S-PD---------";
    /** Enemy Prisoner Of War (EPW) Collection Point */
    String CSS_PNT_EPWCP = "G-S-PE---------";
    /** Logistics Release Point (LRP) */
    String CSS_PNT_LRP = "G-S-PL---------";
    /** Maintenance Collection Point */
    String CSS_PNT_MCP = "G-S-PM---------";
    /** Rearm, Refuel And Resupply Point */
    String CSS_PNT_RRRP = "G-S-PR---------";
    /** Refuel On The Move (ROM) Point */
    String CSS_PNT_ROM = "G-S-PU---------";
    /** Traffic Control Post (TCP) */
    String CSS_PNT_TCP = "G-S-PO---------";
    /** Trailer Transfer Point */
    String CSS_PNT_TTP = "G-S-PI---------";
    /** Unit Maintenance Collection Point */
    String CSS_PNT_UMC = "G-S-PN---------";
    /** General */
    String CSS_PNT_SPT_GNL = "G-S-PSZ--------";
    /** Class I */
    String CSS_PNT_SPT_CLS1 = "G-S-PSA--------";
    /** Class Ii */
    String CSS_PNT_SPT_CLS2 = "G-S-PSB--------";
    /** Class Iii */
    String CSS_PNT_SPT_CLS3 = "G-S-PSC--------";
    /** Class Iv */
    String CSS_PNT_SPT_CLS4 = "G-S-PSD--------";
    /** Class V */
    String CSS_PNT_SPT_CLS5 = "G-S-PSE--------";
    /** Class Vi */
    String CSS_PNT_SPT_CLS6 = "G-S-PSF--------";
    /** Class Vii */
    String CSS_PNT_SPT_CLS7 = "G-S-PSG--------";
    /** Class Viii */
    String CSS_PNT_SPT_CLS8 = "G-S-PSH--------";
    /** Class Ix */
    String CSS_PNT_SPT_CLS9 = "G-S-PSI--------";
    /** Class X */
    String CSS_PNT_SPT_CLS10 = "G-S-PSJ--------";
    /** Ammunition Supply Point (ASP) */
    String CSS_PNT_AP_ASP = "G-S-PAS--------";
    /** Ammunition Transfer Point (ATP) */
    String CSS_PNT_AP_ATP = "G-S-PAT--------";
    /** Moving Convoy */
    String CSS_LNE_CNY_MCNY = "G-S-LCM--------";
    /** Halted Convoy */
    String CSS_LNE_CNY_HCNY = "G-S-LCH--------";
    /** Main Supply Route */
    String CSS_LNE_SLPRUT_MSRUT = "G-S-LRM--------";
    /** Alternate Supply Route */
    String CSS_LNE_SLPRUT_ASRUT = "G-S-LRA--------";
    /** One-Way Traffic */
    String CSS_LNE_SLPRUT_1WTRFF = "G-S-LRO--------";
    /** Alternating Traffic */
    String CSS_LNE_SLPRUT_ATRFF = "G-S-LRT--------";
    /** Two-Way Traffic */
    String CSS_LNE_SLPRUT_2WTRFF = "G-S-LRW--------";
    /** Detainee Holding Area */
    String CSS_ARA_DHA = "G-S-AD---------";
    /** Enemy Prisoner Of War (EPW) Holding Area */
    String CSS_ARA_EPWHA = "G-S-AE---------";
    /** Forward Arming And Refueling Area (FARP) */
    String CSS_ARA_FARP = "G-S-AR---------";
    /** Refugee Holding Area */
    String CSS_ARA_RHA = "G-S-AH---------";
    /** Brigade (BSA) */
    String CSS_ARA_SUPARS_BSA = "G-S-ASB--------";
    /** Division (DSA) */
    String CSS_ARA_SUPARS_DSA = "G-S-ASD--------";
    /** Regimental (RSA) */
    String CSS_ARA_SUPARS_RSA = "G-S-ASR--------";

    //////////////////////////////////////////////
    // Other
    //////////////////////////////////////////////

    /** Ditched Aircraft */
    String OTH_ER_DTHAC = "G-O-ED---------";
    /** Person In Water */
    String OTH_ER_PIW = "G-O-EP---------";
    /** Distressed Vessel */
    String OTH_ER_DSTVES = "G-O-EV---------";
    /** Sea Mine-Like */
    String OTH_HAZ_SML = "G-O-HM---------";
    /** Navigational */
    String OTH_HAZ_NVGL = "G-O-HN---------";
    /** Iceberg */
    String OTH_HAZ_IB = "G-O-HI---------";
    /** Oil Rig */
    String OTH_HAZ_OLRG = "G-O-HO---------";
    /** Bottom Return/Non-Milco */
    String OTH_SSUBSR_BTMRTN = "G-O-SB---------";
    /** Installation/Manmade */
    String OTH_SSUBSR_BTMRTN_INS = "G-O-SBM--------";
    /** Seabed Rock/Stone,  Obstacle,Other */
    String OTH_SSUBSR_BTMRTN_SBRSOO = "G-O-SBN--------";
    /** Wreck,  Non Dangerous */
    String OTH_SSUBSR_BTMRTN_WRKND = "G-O-SBW--------";
    /** Wreck,  Dangerous */
    String OTH_SSUBSR_BTMRTN_WRKD = "G-O-SBX--------";
    /** Marine Life */
    String OTH_SSUBSR_MARLFE = "G-O-SM---------";
    /** Sea Anomaly (Wake, Current, Knuckle) */
    String OTH_SSUBSR_SA = "G-O-SS---------";
    /** Bearing Line */
    String OTH_BERLNE = "G-O-B----------";
    /** Electronic Bearing Line */
    String OTH_BERLNE_ELC = "G-O-BE---------";
    /** Acoustic Bearing Line */
    String OTH_BERLNE_ACU = "G-O-BA---------";
    /** Torpedo, Bearing Line */
    String OTH_BERLNE_TPD = "G-O-BT---------";
    /** Electro-Optical Intercept */
    String OTH_BERLNE_EOPI = "G-O-BO---------";
    /** Acoustic Fix */
    String OTH_FIX_ACU = "G-O-FA---------";
    /** Electro-Magnetic Fix */
    String OTH_FIX_EM = "G-O-FE---------";
    /** Electro-Optical Fix */
    String OTH_FIX_EOP = "G-O-FO---------";
}
