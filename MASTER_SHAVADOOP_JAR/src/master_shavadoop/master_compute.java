package master_shavadoop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class master_compute {

	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub

		// ================================ Données Inputs ====================================
		String login = "wbirmingham"; // = "pi"; "ghost"; //
		String path_cible = "~/workspace/TPTelecom/INF727/"; // Chemin cible
																// pour l'envoi
																// du fichier
																// JAR
		String file_cible = "slave_calcul.jar";
		String ips_source = "liste_ip.txt";
		String input_file = "forestier_mayotte.txt";
		String final_file = "output_final.txt";
		String pool_ips = "137.194.34.";
		int pool_ips_start = 91;
		int pool_ips_end = 98;
		
		// =====================================================================================
		
		
		ArrayList<String> ips_list_ok = new ArrayList<String>();	// list of available worker's IP
		HashMap<String, Double> list_ip_with_time = new HashMap<String, Double>();	// contains available worker's IP and response time 
		HashMap<String, Double> temps_execution_functions = new HashMap<String, Double>();	// contains execution time of program parts
		HashMap<String, String> cles_Sx_machines = new HashMap<String, String>();	// contains name of first split of the input file and theirs workers 
		HashMap<String, String> cles_UMx_machines = new HashMap<String, String>();	// contains name of UMx files and theirs workers  
		HashMap<String, ArrayList<String>> cles_UMx_tmp = new HashMap<String, ArrayList<String>>();	//contains returns of workers with UMx name and words inside
		HashMap<String, HashSet<String>> cles_UMx = new HashMap<String, HashSet<String>>();	//  contains keys of words and their UMx
		HashMap<String, String> cles_SMx_machines = new HashMap<String, String>();	// contains name of SMx file and theirs workers
		HashMap<String, Integer> cles_RMx_machines = new HashMap<String, Integer>();	// contains name of RMx file and theirs workers

		String params = "";	// parameters to send to the worker which contains execution mode (Sx to UMx or UMx to SMx), the word to treat, the SMx name target and the list of UMx name.
		ArrayList<String> params_config = new ArrayList<String>();	// list of parameters to send to workers. 
		ArrayList<String> smx_key_return_tmp = new ArrayList<String>();	// contains returns of workers with SMx name
		ArrayList<String> key_umx_return_tmp = new ArrayList<String>();	// contains untreated returns of workers
		HashMap<String, String> garbage_words = new HashMap<String, String>();	// contains words with treatment error and the error message
		ArrayList<exec_parallele> threads = new ArrayList<exec_parallele>();	// contains list of threads

		
//		start time for all treatment
		double time_start_5 = java.lang.System.currentTimeMillis();
		
//		start time for scan available workers
		double time_start_4 = java.lang.System.currentTimeMillis();
		
		
		System.out.println(
				"\n*******************************  Scan of available IP's workers  *********************************\n");
		
		// Vérification des IPs valides
		// String file_logs = ssh_manager.check_save_goods_ip(login,
		// ips_source);
		//
		// // Création de la liste d'IPs valides
		// ips_list_ok = functions_tool.read_file(file_logs);
		// for(String ip_unformated : ips_list_ok){
		// list_ip_with_time.putAll(ssh_manager.extract_ip_from_logs(ip_unformated));
		// }
		
//		Scan a range of available worker's IP
		list_ip_with_time.putAll(ssh_manager.ip_workers_scanner(login, pool_ips, pool_ips_start, pool_ips_end));
		
//		Sort the list of available worker's IP by response time decreases
		list_ip_with_time = functions_tool.sort_hashmap_by_values(list_ip_with_time);
		
//		Print the list of workers' IP
		for(String ip_scaned : list_ip_with_time.keySet()){
			System.out.println(ip_scaned + " : " + list_ip_with_time.get(ip_scaned) + "s");
		}
		

		// Simulation de test
		// functions_tool.simultation_function(list_ip_with_time, login,
		// file_cible, params);

//		End time for scan available workers
		double time_end_4 = java.lang.System.currentTimeMillis();
		temps_execution_functions.put("ip_scanner_function", (time_end_4 - time_start_4) / 1000.0);
		
//		start time for Splitting Function
		double time_start_1 = java.lang.System.currentTimeMillis();

		System.out.println(
				"\n*******************************  Splitting Function Launched  *********************************\n");
		cles_Sx_machines = functions_tool.splitting_function(list_ip_with_time, login, input_file, path_cible);
		System.out.println("cles_Sx_machines : [" + cles_Sx_machines.size() + " Sx]");
		for (String sx : cles_Sx_machines.keySet()) {
			System.out.println("		" + sx + " : " + cles_Sx_machines.get(sx));
		}

		double time_end_1 = java.lang.System.currentTimeMillis();
		temps_execution_functions.put("splitting_function", (time_end_1 - time_start_1) / 1000.0);

		double time_start_2 = java.lang.System.currentTimeMillis();

		// Execution parallelisée de split_mapping du JAR slave
		// extends de la classe Thread à la classe exec_parallele, pour
		// implémenter la function getLogs_return_tmp()
		System.out.println(
				"\n****************************  Splits Mapping Function Launched  ********************************\n");
		for (String el_splitted : cles_Sx_machines.keySet()) {
			params = " " + "modeSXUMX " + el_splitted;
			exec_parallele thread_ip = new exec_parallele(cles_Sx_machines.get(el_splitted), login, path_cible,
					file_cible, params);
			thread_ip.start();
			threads.add(thread_ip);
			// System.out.println("Fin d'execution :" + params);
		}

		// Attente de la fin d'execution
		for (exec_parallele thread_ip_launched : threads) {
			thread_ip_launched.join();
			for (String log_return : thread_ip_launched.getLogs_return()) {
				key_umx_return_tmp.add(log_return);
			}
		}
		
		double time_end_2 = java.lang.System.currentTimeMillis();
		temps_execution_functions.put("splits_mapping_function", (time_end_2 - time_start_2) / 1000.0);

		// Création du dictionnaire clés-UMx
		for (String keys_tmp : key_umx_return_tmp) {
			ArrayList<String> cles_UMx_sub = new ArrayList<String>();
			for (String el : keys_tmp.split(" ")) {
				if (!el.trim().substring(0, Math.min(el.length(), 2)).equals("UM")) {
					cles_UMx_sub.add(el);
				}
			}
			if (!cles_UMx_tmp.containsKey(keys_tmp.split(" ")[0])) {
				cles_UMx_tmp.put(keys_tmp.split(" ")[0], cles_UMx_sub);
			}
		}

		// Création de la clés-UMx
		for (String key_umx : cles_UMx_tmp.keySet()) {
			for (String key_str_words : cles_UMx_tmp.get(key_umx)) {
				for (String key_word : key_str_words.split(" ")) {
					if (!cles_UMx.containsKey(key_word)) {
						HashSet<String> umx_list_tmp = new HashSet<String>();
						umx_list_tmp.add(key_umx);
						cles_UMx.put(key_word, umx_list_tmp);
					} else {
						cles_UMx.get(key_word).add(key_umx);
					}
				}
			}
		}
		
		System.out.println(
				"\n*******************************  List of cles_UMx_tmp  *********************************\n");
		System.out.println(">	cles_UMx_tmp : 	[" + cles_UMx_tmp.size() + "UMx_tmp]");
		for(String umx_wd_tmp : cles_UMx_tmp.keySet()){
			System.out.println(">	cles_UMx_tmp : 	" + umx_wd_tmp + "		" + cles_UMx_tmp.get(umx_wd_tmp));
		}
		
		
		System.out.println(
				"\n*******************************  List of cles_UMx  *********************************\n");
		System.out.println(">	cles_UMx : 	[" + cles_UMx.size() + "UMx]");
		for(String umx_wd : cles_UMx.keySet()){
			System.out.println(">		cles_UMx : 	" + umx_wd + "		" + cles_UMx.get(umx_wd));
		}
		
		
		double time_start_3 = java.lang.System.currentTimeMillis();

		System.out.println(
				"\n****************************  Shuffling Maps / Reducing Sorted Maps Function Launched  *********************************\n");
		
//		Build one parameter which contain all parameters needed by the slave
		params = " " + "modeUMXSMX";
		int index_smx = 0;
		for (String i_word : cles_UMx.keySet()) {
			String params_smx_name = "SM" + index_smx;
			String extend_params = i_word + " " + params_smx_name;
			for (String i_umx_word : cles_UMx.get(i_word)) {
				extend_params = extend_params + " " + i_umx_word;
			}
			params_config.add(params + " " + extend_params);
			index_smx += 1;
		}

		threads.clear();

		
		int index_select_ip = 0;
		for (String el_param : params_config) {
			String ip_word_selected = "";
			ip_word_selected = list_ip_with_time.keySet().toArray(new String[index_select_ip])[index_select_ip];
			
//			select the worker's IP which have the best response time
			if (index_select_ip < list_ip_with_time.keySet().size() - 1) {
				index_select_ip += 1;
			} else {
				index_select_ip = 0;
			}
			exec_parallele thread_ip = new exec_parallele(ip_word_selected, login, path_cible, file_cible, el_param);
			thread_ip.start();
			threads.add(thread_ip);
		}

//		 Waitting the end of the execution of all threads
		for (exec_parallele thread_ip_launched : threads) {
			thread_ip_launched.join();
			for (String log_return : thread_ip_launched.getLogs_return()) {
				smx_key_return_tmp.add(log_return);
				if (!log_return.isEmpty()) {
					if(!thread_ip_launched.get_params_return().split(" ")[2].equals(log_return.split(" ")[0]) && log_return.split(" ").length > 2){
						System.out.println("bad word : " + thread_ip_launched.get_params_return().split(" ")[2]);
						System.out.println("return msg : " + log_return);
						System.out.println(log_return.split(" ").length);
						garbage_words.put(thread_ip_launched.get_params_return().split(" ")[2], thread_ip_launched.get_params_return());
					}
					else{
						cles_RMx_machines.put(log_return.split(" ")[0],
								Integer.parseInt(log_return.split(" ")[1]));
					}
					
					
					
							//Integer.parseInt(functions_tool.word_cleaner_regex(log_return.split(" ")[1]).trim()));
				}
			}
		}
		System.out.println("==================================================================");
		for(String wd_el : garbage_words.keySet()){
			System.out.println("garbage_words : " + wd_el + " | param : " + garbage_words.get(wd_el) + " | clé : " + cles_UMx.get(wd_el) );
		}
		System.out.println("==================================================================");
		
		System.out.println(
				"\n****************************  REcycle Function Launched  *********************************\n");
	
		index_select_ip = 0;
		for (String wd_el_param : garbage_words.keySet()) {
			String ip_word_selected = "";
			ip_word_selected = list_ip_with_time.keySet().toArray(new String[index_select_ip])[index_select_ip];
			if (index_select_ip < list_ip_with_time.keySet().size() - 1) {
				index_select_ip += 1;
			} else {
				index_select_ip = 0;
			}
			exec_parallele thread_ip = new exec_parallele(ip_word_selected, login, path_cible, file_cible, garbage_words.get(wd_el_param));
			thread_ip.start();
			threads.add(thread_ip);
		}
		
		// Attente de la fin d'execution de tous les threads
		for (exec_parallele thread_ip_launched : threads) {
			thread_ip_launched.join();
			for (String log_return : thread_ip_launched.getLogs_return()) {
				smx_key_return_tmp.add(log_return);
				if (!log_return.isEmpty()) {
					if(!thread_ip_launched.get_params_return().split(" ")[2].equals(log_return.split(" ")[0]) && log_return.split(" ").length!=2){
						System.out.println("recycle bad word : " + thread_ip_launched.get_params_return().split(" ")[2]);
						System.out.println("recycle return msg : " + log_return);
						System.out.println(log_return.split(" ").length);
//						garbage_words.put(thread_ip_launched.get_params_return().split(" ")[2], thread_ip_launched.get_params_return());
					}
					else{
						cles_RMx_machines.put(log_return.split(" ")[0],
								Integer.parseInt(log_return.split(" ")[1]));
					}
				}
			}
		}
		
		System.out.println("*****************************	smx_key_return_tmp	*************************************");
		for(String smx_wd : smx_key_return_tmp){
			System.out.println("smx_key_return_tmp : " + smx_wd);
		}
		System.out.println("*****************************		RMx returns		**************************************");
		// System.out.println("Fin d'execution : " + params);
		for(String rmw_wd : cles_RMx_machines.keySet()){
			System.out.println("RMx returns : " + rmw_wd + " : " + cles_RMx_machines.get(rmw_wd));
		}
		System.out.println("*******************************************************************");
		
		double time_end_3 = java.lang.System.currentTimeMillis();
		temps_execution_functions.put("reducing_sorted_maps_function", (time_end_3 - time_start_3) / 1000.0);

		new File(final_file).delete();
		for (String final_key_word : cles_RMx_machines.keySet()) {
			String str_final_word = final_key_word + " " + cles_RMx_machines.get(final_key_word);
			functions_tool.write_file(final_file, str_final_word);
		}
		System.out.println("*	réation du fichier : " + final_file);

//		count total of words from the input file
		int tot = 0;
		for (int el : cles_RMx_machines.values()) {
			tot += el;
		}
		
//		Chrono all treatments 
		double time_end_5 = java.lang.System.currentTimeMillis();
		temps_execution_functions.put("master_treatment", (time_end_5 - time_start_5) / 1000.0);
		
		System.out.println("*******************************************************************");
		System.out.println("*	WORD COUNT REPORT : " + tot);
		System.out.println("*	Temps d'execution Global : " + temps_execution_functions.get("master_treatment"));
		System.out.println("*------------------------------------------------------------------");
		System.out.println("*	Temps d'execution Scan Available Workers : " + temps_execution_functions.get("ip_scanner_function"));
		System.out.println("*	Temps d'execution Splitting : " + temps_execution_functions.get("splitting_function"));
		System.out.println("*	Temps d'execution Splits Mapping : " + temps_execution_functions.get("splits_mapping_function"));
		System.out.println(
				"*	Temps d'execution Reducing Sorted Maps : " + temps_execution_functions.get("reducing_sorted_maps_function"));
		System.out.println("*******************************************************************");
	}

}
