package etomica.models.nitrogen;

/**
 * The parameters that are determined through conjugate gradient method to give the lowest lattice energy
 *  for beta-N2 phase structure
 * 
 * 
 * 	for lattice sum
 * 
 * @author taitan
 *
 */
public class BetaPhaseLatticeParameterLS {
	public BetaPhaseLatticeParameterLS(){
		
	}
	
	public double[][] getParameter(double density){
		double[][] parameters;
		
		// these parameters are for nA=1024
	if(density==0.0240){
		parameters = new double[][]{
//				New Lattice Energy (per molecule): -816.3897306623942
				{0.004147557643222644, -0.001006463440200005, 8.261562240251194E-4, -4.4376305144395496E-4, -0.005953612450064236},
				{-0.004862174402200582, -0.0011092591910246072, 8.204124676007498E-4, 2.870729851652848E-4, -0.00601751953158228},
				{-0.004825084311304207, -9.987619884158534E-4, 8.327115239091108E-4, -1.021093584449469E-4, -0.006150556436931998},
				{0.004126452623147835, -0.0011320893744204926, 8.348948728663177E-4, 3.744773269603735E-4, -0.005967165278386102}
				
		};
	} else if (density==0.0239){
		parameters = new double[][]{
//				New Lattice Energy (per molecule): -818.3648168890657
				{0.005784244636272419, 0.0017352435788928512, -2.8764414814617237E-5, -8.406579647231106E-4, -0.007045499320184655},
				{-0.004218014746795483, 0.0013933742725164276, -3.450816595039935E-5, 6.838631741410623E-4, -0.007109444650359114},
				{-0.0041809238656802875, 0.0017429463558708345, -2.220746876970254E-5, -4.988469562721469E-4, -0.007242499967532442},
				{0.005763138828921909, 0.0013705427656494767, -2.00241198035687E-5, 7.715162781609096E-4, -0.007058999208731853}
		
		};
	} else if (density==0.0238){
		parameters = new double[][]{
//				New Lattice Energy (per molecule): -820.1829834416721
				{0.00628246092054765, 0.0018531486021145828, -3.9074322193266274E-5, -0.0012379084551976402, -0.008311398201523247},
				{-0.004716230322236202, 0.001275423526452628, -4.4818057754682564E-5, 0.0010809925797818423, -0.00837538185128054},
				{-0.004679138860027833, 0.001860851925390573, -3.2515929872076944E-5, -8.959177145508405E-4, -0.008508454621150002},
				{0.006261354538545272, 0.0012525914530612942, -3.033250085055487E-5, 0.0011689359464616398, -0.00832484422394071}
		
		};
	} else if (density==0.0237){
		parameters = new double[][]{
//				New Lattice Energy (per molecule): -821.8438991132392
				{0.0054727334682842885, 2.737568496463047E-4, -1.141455434670669E-5, -0.0017027988515957142, -0.009621239183938998},
				{-0.006433873100462061, -4.940789812369578E-4, -1.7158494455169924E-5, 0.0015457582618421581, -0.009685268182038081},
				{-0.0063967812863510165, 2.814605767061107E-4, -4.8552667062985665E-6, -0.0013606246492802827, -0.009818362196603751},
				{0.005451626721485248, -5.169115571423467E-4, -2.67173886689707E-6, 0.0016340031266112193, -0.009634622947207896}
			
		};
	} else if (density==0.0236){
		parameters = new double[][]{
//				New Lattice Energy (per molecule): -823.3475183133554
				{0.005880060360390368, 3.19678448439234E-4, -1.540726855493523E-5, -0.0022577485885724234, -0.010969098514152278},
				{-0.006841199651801188, -5.39897479943176E-4, -2.1151269191786335E-5, 0.002100579722824476, -0.01103318086169814},
				{-0.006804107302078144, 3.2738266606038846E-4, -8.846345236443386E-6, -0.001915384837855614, -0.011166298805011918},
				{0.005858953079326691, -5.627305913233406E-4, -6.6627546047890265E-6, 0.0021891338502896722, -0.01098240639497719}
			
		};
	} else if (density==0.0235){
		parameters = new double[][]{
//				New Lattice Energy (per molecule): -824.6935233784759
				{0.007210526881811468, 0.0032913718997018295, 1.770918493766453E-4, -0.0028652326655028092, -0.012359406795320604},
				{-0.00620652514104287, 0.0023197633571041556, 1.7134875975119846E-4, 0.002707929075598013, -0.012423543090368158},
				{-0.0061694325069809606, 0.0032990769858712286, 1.8365357286843562E-4, -0.002522671911572397, -0.012556689192686592},
				{0.007189419319425556, 0.0022969299203138183, 1.8583649029910686E-4, 0.0027968049657997205, -0.012372634404895099}
	
		};
	} else if (density==0.0234){
		parameters = new double[][]{
//				New Lattice Energy (per molecule): -825.8818915835194
				{0.00750188186214623, 0.003389049486872417, 1.793979693313933E-4, -0.0034663219391267187, -0.013787937251400104},
				{-0.006497905349495855, 0.0022221970749718447, 1.7365412006406934E-4, 0.0033088844872615707, -0.01385213392254552},
				{-0.006460811235791093, 0.0033967550955585333, 1.8595986918952187E-4, -0.0031235639331715095, -0.013985304597095086},
				{0.007480772810274266, 0.0021993626947350753, 1.8814325563918342E-4, 0.0033980871972870506, -0.013801081491599837}
	
		};
	} else if (density==0.0233){
		parameters = new double[][]{
//				New Lattice Energy (per molecule): -826.9134650962379
				{0.007337770942247262, 0.002040513212632978, 7.345885990242668E-4, -0.003995270043755013, -0.015241648145206644},
				{-0.007186362698790597, 6.839451752541777E-4, 7.288449056165485E-4, 0.0038376941446584003, -0.015305894042670364},
				{-0.007149266870086652, 0.002048219805107554, 7.411513012152222E-4, -0.0036523084245389283, -0.015439087424012945},
				{0.007316660165178613, 6.611099239126931E-4, 7.433344753018101E-4, 0.0039272305776971266, -0.015254720699401304}
		
		};
	} else if (density==0.0232){
		parameters = new double[][]{
//				New Lattice Energy (per molecule): -827.7888475230048
				{0.007558799869043709, 0.002135488551603646, 7.342025548574945E-4, -0.004430769685708061, -0.01673852071324079},
				{-0.00740739552579263, 5.889526903365557E-4, 7.284588198047831E-4, 0.00427305221650042, -0.016802807997494888},
				{-0.00737030393404014, 0.0021431928235634, 7.407633721598618E-4, -0.004087629146035944, -0.016936015095954503},
				{0.007537693322093836, 5.661197325048976E-4, 7.42946596993227E-4, 0.0043629608473039535, -0.016751529570221146}
		
		};		
	} else if (density==0.0231){
		parameters = new double[][]{
//				New Lattice Energy (per molecule): -828.5092200773297
				{0.00841779304901295, 0.0025618413611532276, 0.0016544807236760011, -0.004833792434421931, -0.01815897068408562},
				{-0.006994764064837764, 8.621607215663679E-4, 0.0016487372702116152, 0.004675940363078805, -0.018223295348139212},
				{-0.006957670908527389, 0.0025695464511665857, 0.0016610427253339098, -0.004490454510971228, -0.018356516947826796},
				{0.008396684949921174, 8.39327032834929E-4, 0.001663225992910312, 0.0047661748242753136, -0.018171922703403398}
	
		};	
		} else if (density==0.0230){
			parameters = new double[][]{
				//LS -829.0482868398009
//				{0.008643467781612603+7.598872914479568E-6, 0.0026271868875943002+6.048141457007383E-6, 0.0019564392850298443+6.838284955889944E-8, -0.005368542841198517, -0.01963128167700018}, 
//				{-0.007220434499475046-7.559959074754374E-6, 7.968809489413192E-4-6.027060556634822E-6, 0.0019507333095460664-1.0851786136356623E-8, 0.0052101555943571045, -0.019695510442517328}, 
//				{-0.00718296472671309-7.79735625000555E-6, 0.002634905247602907+6.068598038755368E-6, 0.001963151739179857-3.767866326343135E-8, -0.005024039971913238, -0.019828787213698948}, 
//				{0.008622065304767177+7.758442437536331E-6, 7.740172242212951E-4-6.089678856291414E-6, 0.0019653547372104462-1.9852370414241705E-8, 0.005300512113324435, -0.019643975672095665}
		
				// LS 250A New Lattice Energy (per molecule): -829.0759547274187
					{0.008638238585015215, 0.002609639254046046, 0.001965314488403982, -0.005200108308719864, -0.019493918440965832},
					{-0.007224880085926693, 7.955618967001389E-4, 0.001959560667164399, 0.005042160286804371, -0.01955833186721361},
					{-0.007187786705115524, 0.0026173418917451448, 0.001971865526394853, -0.0048566189407208485, -0.01969156893623954},
					{0.008617130022153292, 7.727256988347253E-4, 0.0019740590182580047, 0.005132671969548819, -0.019506822214123858}
				
			};	
		} else if (density==0.0229){
			parameters = new double[][]{
					{0.008870450174064865, 0.0026560151253085737, 0.0017106740786906396, -0.005525619648739416, -0.02073461293889388},
					{-0.007447391594180817, 7.680386763663832E-4, 0.0017049298850897185, 0.00536752742976449, -0.020799005706329046},
					{-0.0074102959757679195, 0.0026637213196452104, 0.001717235545437257, -0.005181930448091992, -0.020932259657012967},
					{0.008849339640350144, 7.452035825149239E-4, 0.0017194193836850957, 0.005458351600347379, -0.020747474368700933},
//					Old Lattice Energy (per molecule): -829.4904038470147
//					New Lattice Energy (per molecule): -829.4904038467461

			};	
		 
		 } else if (density==0.0228){
			parameters = new double[][]{
					{0.009099901021889711, 0.002671950820769646, 0.0017108228544796245, -0.005806274603031318, -0.02187251208085202},
					{-0.007676844216761822, 7.521048037814665E-4, 0.0017050777979179263, 0.005648077875566186, -0.021936935642670595},
					{-0.0076397471881696185, 0.002679657347955059, 0.0017173846016798372, -0.00546243021760674, -0.022070198505670793},
					{0.009078789085878045, 7.29268609665239E-4, 0.0017195692624025326, 0.005739159124348575, -0.02188533288013945},
//					rC= 250A Old Lattice Energy (per molecule): -829.7538873035504
//					New Lattice Energy (per molecule): -829.7538873040055

			};	
		} else if (density==0.0227){
			parameters = new double[][]{
					{0.009331489010328283, 0.002666222517180565, 0.0017106614792465767, -0.006038247974364204, -0.022899654697951334},
					{-0.00790842974115644, 7.578332279316086E-4, 0.0017049173981622265, 0.005879952079616128, -0.022964094576741426},
					{-0.00787133190305422, 0.0026739299026024045, 0.001717224339652281, -0.005694261407959041, -0.023097367158020028},
					{0.009310376256532964, 7.34997047415169E-4, 0.0017194080650071008, 0.0059712727005078155, -0.02291244442358184},
//					Old Lattice Energy (per molecule): -829.8676987698226
//					New Lattice Energy (per molecule): -829.8676987701025

			};	
		} else if (density==0.0226){
			parameters = new double[][]{
					{0.009565293691688681, 0.0026383109684242206, 0.001711048123578971, -0.006217927211024235, -0.0238086259493167},
					{-0.00814223533417555, 7.857456517698696E-4, 0.001705302701776574, 0.006059550029823872, -0.02387308968839983},
					{-0.008105136151423605, 0.00264601845795576, 0.0017176105432230343, -0.0058738180609221235, -0.024006368289880457},
					{0.009544179603367276, 7.629081654515191E-4, 0.0017197955694827193, 0.006151072881294514, -0.023821390188307097},
//					rC= 250A Old Lattice Energy (per molecule): -829.833108659562
//					New Lattice Energy (per molecule): -829.8331086596361

			};	
		} else if (density==0.0225){
			parameters = new double[][]{
					{0.00980144215170342, 0.002587831643947547, 0.0017100426520679569, -0.0063419853982132806, -0.02459272128753842},
					{-0.008378380426031094, 8.362238671248462E-4, 0.0017042988391075828, 0.006183530026617107, -0.02465718760431603},
					{-0.008341280467964892, 0.0025955402721147304, 0.0017166063842665488, -0.00599776478408633, -0.024790473735329954},
					{0.009780327277589219, 8.133866935814127E-4, 0.0017187898414902768, 0.0062752375255556245, -0.0246054715159445},
//					Old Lattice Energy (per molecule): -829.6513708742847
//					New Lattice Energy (per molecule): -829.6513708744932


			};
		} else if (density==0.0224){
			parameters = new double[][]{
					{0.010040094772666002, 0.0025145422282479776, 0.0017110449524495469, -0.006407389608139119, -0.025245963541686553},
					{-0.008617037704831108, 9.095135427756357E-4, 0.0017052996596993173, 0.00624887746229487, -0.025310443824416436},
					{-0.008579937261708467, 0.0025222504435455963, 0.0017176077796567698, -0.006063084969343665, -0.025443730906942404},
					{0.010018979423242012, 8.866754250818887E-4, 0.0017197926769693794, 0.006340731063519104, -0.02525870358144099},
//					rC= 250A Old Lattice Energy (per molecule): -829.3237369690598
//					New Lattice Energy (per molecule): -829.3237369686045

			};	
		} else if (density==0.0223){
			
			parameters = new double[][]{
					{0.010281464274035345, 0.0024183379896466142, 0.0017112064988988292, -0.006411455032742008, -0.025763198278863666},
					{-0.00885841486230696, 0.0010057174220687354, 0.0017054621955374391, 0.006252891588147591, -0.025827673118706377},
					{-0.008821314293659618, 0.0024260467405590215, 0.0017177701596604719, -0.006067078979502799, -0.025960960436639636},
					{0.010260348790392124, 9.828796955554098E-4, 0.0017199541024876652, 0.0063448685422799736, -0.025775938302315053},
//					Old Lattice Energy (per molecule): -828.8514541273611
//					New Lattice Energy (per molecule): -828.8514541272908

			};	
		} else if (density==0.0222){
			parameters = new double[][]{
					{0.010525814085316179, 0.0022992522000237353, 0.001712661414328851, -0.006351827045460941, -0.026140061350870228},
					{-0.009102758147962773, 0.0011248027498533314, 0.0017069159775455487, 0.006193231657326322, -0.02620453658802505},
					{-0.009065656779211447, 0.002306960865795519, 0.001719224707107849, -0.006007400696091698, -0.02633782032074257},
					{0.010504697811878861, 0.0011019640377337425, 0.0017214097370125813, 0.006285289554922074, -0.026152808882228235},
//					rC= 250A Old Lattice Energy (per molecule): -828.235770850517
//					New Lattice Energy (per molecule): -828.2357708503666

			};
		} else if (density==0.02215){
			parameters = new double[][]{
//					{0.01065515858952696, 0.002234475952141081, 0.001713060089195674, -0.0062750869924165864, -0.026288897597283496},
//					{-0.009232085863616766, 0.0011895840058646129, 0.0017073395262763033, 0.006116464184531484, -0.026353378037561815},
//					{-0.009195273912664944, 0.0022541311489263673, 0.0017222566226499413, -0.005932478584067296, -0.026487292673205497},
//					{0.010634335058133687, 0.0011547991809302816, 0.0017244648452212196, 0.00621043940862464, -0.026302252099208515},
////					Old Lattice Energy (per molecule): -827.8745703029435
////					New Lattice Energy (per molecule): -827.8745939582366

			};
		} else if (density==0.0221){
			parameters = new double[][]{
					{0.010772591541114446, 0.0021535144740529033, 0.0017181192569727915, -0.006210287890500496, -0.0263359885833344},
					{-0.009349267527877212, 0.0012705073914771098, 0.0017123987762259494, 0.006051596692287163, -0.02640030860842352},
					{-0.009312170704775965, 0.0021612325990949567, 0.0017247059503977394, -0.005865781296583828, -0.02653358023243978},
					{0.0107514797421083, 0.001247683046750352, 0.001726865926888933, 0.006143800383877867, -0.026348750099446223},
//					rC= 250A Old Lattice Energy (per molecule): -827.4779292334823
//					New Lattice Energy (per molecule): -827.4779292335065

			};	
		} else if (density==0.0220){
			parameters = new double[][]{
//					LS 100A New Lattice Energy (per molecule): -826.5526923011495
//					{0.01102412333106896, 0.001991382618779798, 0.001797586538782932, -0.006025075032917587, -0.026441069565444765},
//					{-0.009601080099358426, 0.001432699868664405, 0.0017918430596670374, 0.005866447581103917, -0.02650551228622455},
//					{-0.009564051544528215, 0.0019990425278673153, 0.0018041237141515289, -0.005681069827722257, -0.026638636798878058},
//					{0.011003080410698609, 0.0014099098903693475, 0.0018063091895190297, 0.005959046849731228, -0.02645371884328682}
					
//					LS 250A New Lattice Energy (per molecule): -826.5789928890252
					{0.011024179733712256, 0.0019913127095949334, 0.0017986516538950035, -0.006025707731887102, -0.026440726328363817},
					{-0.009601124010821336, 0.0014327707404976958, 0.0017929022256632796, 0.005867072643376054, -0.026505165088431072},
					{-0.00956402662052935, 0.0019990195779853205, 0.0018052161562991246, -0.005681251805411911, -0.026638435852129265},
					{0.011003065845172622, 0.0014099333728448671, 0.0018073947148619773, 0.005959229227574842, -0.026453515689983404}
					
			};	
			
		} else if (density==0.0219){
			parameters = new double[][]{
//					New Lattice Energy (per molecule): -825.5410597607662
					{0.01128371577459526, 0.0018221974508320637, 0.0017959882230502875, -0.0058380884066529205, -0.026546330737094913},
					{-0.009860645248847642, 0.001601915732978026, 0.001790244130752878, 0.0056794505079369876, -0.026610748814028473},
					{-0.009823547586018996, 0.0018299047580037121, 0.0018025511031212512, -0.0054936229583023855, -0.02674400899189181},
					{0.011262603190407278, 0.0015790796284778908, 0.0018047347524738477, 0.005771627162118565, -0.02655914726389398},
					
			};	
		
		} else if (density==0.0218){
			parameters = new double[][]{
//					New Lattice Energy (per molecule): -824.3663374573651
					{0.011551392679452202, 0.001645917211978959, 0.0017911944713559014, -0.005647538876095886, -0.02665282399584919},
					{-0.010128301644120017, 0.0017782313281128942, 0.001785450780687565, 0.005488889461211478, -0.026717221201306204},
					{-0.010091203911460563, 0.0016536247721818977, 0.001797758009780497, -0.005303057298889706, -0.026850472682822476},
					{0.011530280019022465, 0.0017553953302854297, 0.0017999411737652082, 0.005581092185666351, -0.02666566613480893}

			};
			
		} else if (density==0.0217){
			parameters = new double[][]{
//					New Lattice Energy (per molecule): -823.0570291911624
					{0.011827546281560717, 0.0014621690761053643, 0.001786486410748696, -0.005453986153544707, -0.02676023690583416},
					{-0.010404434625185675, 0.001962006894579987, 0.001780743134204705, 0.00529532508688572, -0.026824612856529425},
					{-0.01036733707673709, 0.0014698767448455587, 0.0017930501121003056, -0.005109489807897345, -0.02695785745469073},
					{0.011806433798814754, 0.0019391711711674578, 0.0017952327753429486, 0.005387555678862079, -0.026773107013213622}

			};
			
		} else if (density==0.0216){
			parameters = new double[][]{
//					New Lattice Energy (per molecule): -821.6153234498252
					{0.012112536251972297, 0.001270650652800534, 0.0017808725491004183, -0.005257408760579525, -0.02686862061352142},
					{-0.01068940348762901, 0.0021535581267375144, 0.0017751297015819742, 0.005098735895642698, -0.026932974910289776},
					{-0.010652305810511794, 0.0012783586386595526, 0.0017874372334472225, -0.004912895590882076, -0.02706620912444719},
					{0.012091423633876932, 0.0021307224956833284, 0.0017896193787958047, 0.00519099276760575, -0.026881515736180046}
					
			};
			
		} else if (density==0.0215){
			parameters = new double[][]{
//					New Lattice Energy (per molecule): -820.0434102210183
					{0.01240674181543638, 0.001071039254603695, 0.0017742462489999178, -0.005057793761482518, -0.026978034164915142},
					{-0.010983587109713453, 0.002353211019753892, 0.0017685038458090394, 0.004899108942476652, -0.027042366398948987},
					{-0.010946489300120429, 0.0010787475474669551, 0.0017808113118003145, -0.0047132638288405245, -0.027175593432770248},
					{0.012385629057968837, 0.0023303755197886236, 0.001782992920209071, 0.004991392654756233, -0.026990958055963125}

			};
			
		} else if (density==0.0214){
			parameters = new double[][]{
//					New Lattice Energy (per molecule): -818.3434649697299
					{0.012710562731913063, 8.629885678973145E-4, 0.0017667724292466163, -0.004855127593916206, -0.027088531990321414},
					{-0.011287385293602379, 0.0025613058884967396, 0.0017610304851480582, 0.004696430662173431, -0.027152841754922368},
					{-0.01125028738407818, 8.706971535492637E-4, 0.001773337708064153, -0.004510580958413198, -0.02728606236482278},
					{0.01268944986756187, 0.0025384705635452523, 0.0017755187616174794, 0.004788741788103875, -0.027101485957991826}

			};
			
		} else if (density==0.0213){
			parameters = new double[][]{
//					New Lattice Energy (per molecule): -816.5176531826509
					{0.013024421275202622, 6.461323034252647E-4, 0.0017596485628935375, -0.004649406154784585, -0.027200174546505338},
					{-0.011601220677701394, 0.002778200258387325, 0.0017539070909344543, 0.004490696950732245, -0.027264461443747195},
					{-0.011564122423571962, 6.538413346268736E-4, 0.0017662142787327607, -0.004304841294218049, -0.02739767514455651},
					{0.013003308058958635, 0.0027553649843093655, 0.0017683947617416883, 0.004583034492346956, -0.027213158659101126}

			};
			
		} else if (density==0.0212){
			parameters = new double[][]{
//					New Lattice Energy (per molecule): -814.5681406598537
					{0.013348763127869864, 4.200877906709727E-4, 0.0017533835644605478, -0.004440619836419699, -0.027313026791550787},
					{-0.011925539327430654, 0.003004274937224943, 0.0017476425748232669, 0.004281898203757944, -0.027377290439088882},
					{-0.011888440534853714, 4.2779739790855045E-4, 0.001759950097159721, -0.00409603544352519, -0.027510495668290093},
					{0.013327649365407288, 0.0029814396099080145, 0.0017621299974801118, 0.004374261382484538, -0.027326040041367003}
					
			};
			
		} else if (density==0.0211){
			parameters = new double[][]{
//					New Lattice Energy (per molecule): -812.4970808876758
					{0.013684061094042778, 1.8445489651738867E-4, 0.0017474883607307304, -0.004228768773549268, -0.027427161754052045},
					{-0.012260811749892329, 0.003239935144376987, 0.0017417478515510962, 0.004070034586686519, -0.027491401836150415},
					{-0.01222371216139739, 1.921652070702197E-4, 0.0017540555206795449, -0.003884163457451778, -0.02762460031105341},
					{0.013662946527815698, 0.0032170996442018865, 0.001756234840123173, 0.004162422493683185, -0.027440206413530063}

			};
			
		} else if (density==0.0210){
			parameters = new double[][]{
//					New Lattice Energy (per molecule): -810.3066044768439
					{0.01403062873901948, -6.137724652901383E-5, 0.0017709751910001466, -0.004013820498890797, -0.027542654211713886},
					{-0.012607714599277762, 0.0034854168402342397, 0.0017652320821357212, 0.0038550847849153536, -0.027606890968158156},
					{-0.012570620622228575, -5.367124450094893E-5, 0.00177753738050719, -0.0036692383542734452, -0.02774007519144925},
					{0.01400951982611862, 0.003462583073807173, 0.00177971984379517, 0.0039475197261825586, -0.027555723283217964}

			};
		}else {
			throw new RuntimeException("<BetaPhaseLatticeParameterLS> Sorry I do not have the parameters you want!! " +
										"\n                         You have to do your own minimization procedure!!! HA! HA!");
		}
		
		
		return parameters;
	}
	
}
