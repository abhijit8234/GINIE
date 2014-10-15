/**
 * java ProcessDnsNames <table_name_from_where_to_pick_dnsnames> <ISP Name>
 * table_name_from_where_to_pick_dnsnames should have a column pickedup set to 'N'
 * ISP name is just to group the results that are stored.   
 * Processes the DNS names to identify information in them.
 * It loads all the interface, router, city, state and other information in local HashMap and checks individual sections of DNS names against them.
 * It needs a high memory as we are loading a lot of data into hashmaps (Exact memory requirements has to be measured)
 * 
 * Change Log:
 * bug RM: disallowing multiple city checks.
 * 
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class ExecProcessDnsNames implements Runnable
{
	int intfCounter=0;
	int routerCounter=0;
	int airport_codeCounter=0;
	int cityCounter=0;
	int state_decodeCounter=0;
	int city_clliCounter=0;
	int othersCounter=0; 
	boolean city_clli_reco=false, city_reco=false,airport_code_reco=false;
	String[] ips;
	String[] DnsNames;
	String isp;
	String intf="",router="",airport_code="",city="",state_decode="", city_clli="", domain="", other="";
	
	int intf_found = -1,router_found=-1, airport_code_found = -1,city_found= -1,state_found = -1, city_clli_found =-1;
	
	//ArrayList<String> others = new ArrayList<String>();
	PreparedStatement pstmt;
//	PreparedStatement pstmt2;
//	PreparedStatement pstmt3;
	
	ExecProcessDnsNames(/*String[] ips,*/String[] dnsnames,String isp) throws SQLException
	{
		this.ips = ips;
		this.DnsNames = dnsnames;
		this.isp = isp;
		pstmt =DataBaseManager.getConn().prepareStatement("insert delayed into processedNames_batch(isp,dnsname,property,value,value_code,`index`) values(?,?,?,?,?,?)");
	}
	
	//@Override
	public void run() {
		int index=0;
		for (String name: DnsNames) //within a string
        {
			//String ip = ips[index++];
			String ip = "";
			int count=0,size;
			boolean initialFlag=true;
			boolean noneFlag=true;
			name = name.toLowerCase();
			//block to induce whitespace in domains which have hyphen. By common observation, the last two segments have the company domain like something.com
			//sometimes these domains might be separated by "-" and we want such company names to be treated as one entity
			String[] tempnameArr = name.split("\\.");
			if(tempnameArr.length > 2 && tempnameArr[tempnameArr.length-2].contains("-"))
			{
				if (tempnameArr[tempnameArr.length-2].split("-").length==2)
				{
					tempnameArr[tempnameArr.length-2] = tempnameArr[tempnameArr.length-2].replace("-", " ");
					StringBuffer newname = new StringBuffer();
					for (String s:tempnameArr)
					{
						newname.append(s+".");
					}
					name = newname.toString();
				}
			}
			
			//block to induce white space ends
			//block for cases where there are no dots. (or one dot at the end) : 1dot -starts
			String[] parts = name.split("\\.");
			int len_domain=2;
			if(parts.length==1)
			{
				domain = name;
				parts = name.split("-|\\.");
				len_domain=0;
				size = parts.length;
			}
			// 1dot-ends
			else
			{
				parts = name.split("-|\\."); // First pass splits on . and -
				int len = parts.length;
				if (parts.length > len_domain) {
					if (parts[len - 1].length() == 2)
						len_domain = 3;
				}

				size = parts.length - len_domain;
				if (parts.length > len_domain) {
					for (int i = len_domain; i > 0; i--) {
						if (!parts[parts.length - i].matches("\\d"))
							domain = domain + parts[parts.length - i] + ".";
					}
					domain = domain.substring(0, domain.length() - 1);
				} else
					domain = name;
			}
			for(int i=0;i<parts.length-len_domain;i++)
			{
				String part = parts[i],nextpart="";
				if (i<parts.length-3)
					nextpart = parts[i+1]; 
				//if initial part of the name is a number, ignore it. Hence the initialFlag.
				if(part.matches("\\d+") && initialFlag) 
				{
					size--; //reduce the size as we ignore the initial part with numbers.
					continue; // bug RM
				}
				else
					initialFlag=false;
				
				count++; //counting the position of the dns name split array
				noneFlag=true; //set to true for each iteration
				if (size==0 || count<((size+2)/2+1))
				{
					intfCounter ++;
					if (ProcessDnsNames_batch.intfMap.containsKey(part.replaceAll("\\d+$","").replaceAll("^\\d+","")))
					{
						int interface_found = name.indexOf("."+part)+1;
						if(interface_found==0)
							interface_found = name.indexOf("-"+part)+1;
						
						intf = intf + part + "|" +interface_found+":";
						noneFlag=false;
						continue;
					}
				}	
				
				routerCounter ++;
				if (ProcessDnsNames_batch.routerMap.containsKey(part.replaceAll("\\d+$","").replaceAll("^\\d+","")))
				{
					
					router_found = name.indexOf("."+part)+1;
					
					if (router_found ==0)
						router_found = name.indexOf("-"+part)+1;
					
					router = router + part + "|" + router_found + ":";
					noneFlag=false;
				}
				
				airport_codeCounter ++;
				if (ProcessDnsNames_batch.airport_code_decodeMap.containsKey(part.replaceAll("\\d+$","").replaceAll("^\\d+",""))) 
				{
					//int prev_found = airport_code_found;
					//String prev_airport = airport_code;
					airport_code_found = name.indexOf("."+part)+1;
					if (airport_code_found == 0)
			    		airport_code_found = name.indexOf("-"+part)+1;
						
					airport_code = airport_code + part +"|"+ airport_code_found + ":"; //+"("+airport_code_found+")";
					
					noneFlag = false;
					continue; //bug RM	
						
				}
				cityCounter ++;
				if (ProcessDnsNames_batch.cityMap.containsKey(part.replaceAll("\\d+$","").replaceAll("^\\d+",""))) 
				{ 
					city_found = name.indexOf(part);
					city = city + part + "|"+city_found +":" ; //+"("+city_found+")";
						
					noneFlag = false;
					continue; // bug RM									
				}
				city_clliCounter ++;
				
				if (ProcessDnsNames_batch.city_clliDecodeMap.containsKey(part.replaceAll("\\d+$","").replaceAll("^\\d+",""))) 
				{ 
					int city_clli_found = name.indexOf(part);
					
					city_clli = city_clli+ part+"|"+city_clli_found+":" ; //+"("+city_clli_found+")";
					noneFlag = false;
					continue;
					
				}
				state_decodeCounter ++;
				if (ProcessDnsNames_batch.state_decodeMap.containsKey(part.replaceAll("\\d+$","").replaceAll("^\\d+",""))) {
					int state_found = name.indexOf("."+part)+1;
					if (state_found==0)
						state_found = name.indexOf("-")+1;
					state_decode = state_decode + part +"|"+ state_found+":";
					noneFlag = false;
				}
				othersCounter++;
				if(noneFlag && !part.matches("\\d*"))
				{
					int other_found = name.indexOf("."+part)+1;
					if (other_found==0)
						other_found = name.indexOf("-"+part)+1;
					other = other+part+"|"+other_found+":";
				}
			}
			
			partSplit(name,len_domain);
			
			try 
			{				
				printall(ip,name,pstmt);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			resetall();
        }
		synchronized(this)
		{
			try 
			{
				pstmt.executeBatch();
			//	pstmt2.executeBatch();
			//	pstmt3.executeBatch();
			}catch (SQLException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	private void checkDictionaryAndInsert(String ipaddr,String dnsname,String others) throws SQLException
	{
		String words = "";
		String others_new = "";
		
		for(String other:others.split(":"))
		{
			if(!other.equals(""))
			{
				String[] oth = other.split("\\|");
				if(!oth[0].equals("IGNORE"))
				{
					if (ProcessDnsNames_batch.dictMap.containsKey(oth[0]))
					{
						words = words + oth[0]+"|"+oth[1]+":";
					}
					else
					{
						others_new = others_new + oth[0]+"|"+oth[1]+":";
					}
				}
			}
		}
		
		others = others_new;
		
		if(!words.equals(""))
		{
			for(String in : words.split(":"))
			{
				String[] i = in.split("\\|");
				pstmt.setString(1, isp);
				pstmt.setString(2, dnsname.replaceAll(" ", "-")); //replaces the induced space
				pstmt.setString(3, "Word");
				pstmt.setString(4, i[0]);
				pstmt.setString(5, i[0].replaceAll("\\d+$","").replaceAll("^\\d+",""));
				pstmt.setInt(6, new Integer(i[1]));
				pstmt.addBatch();
			}
		}
		
		if(!others.equals(""))
		{
			for(String in : others.split(":"))
			{
				String[] i = in.split("\\|");
				pstmt.setString(1, isp);
				pstmt.setString(2, dnsname.replaceAll(" ", "-")); //replaces the induced space
				pstmt.setString(3, "Other");
				pstmt.setString(4, i[0]);
				pstmt.setString(5, i[0].replaceAll("\\d+$","").replaceAll("^\\d+",""));
				pstmt.setInt(6, new Integer(i[1]));
				pstmt.addBatch();
			}
		}
	}
	
	private void printall(String ip,String name,PreparedStatement pstmt) throws SQLException
	{
		//System.out.println(name + " intf=" + intf + " router="+ router  + " airport_code=" + airport_code + " city="+ city + " state_decode=" + state_decode + " " + "city_clli="+city_clli);
		//PreparedStatement pstmt =DataBaseManager.getConn().prepareStatement("insert delayed into processedNames(dnsname,domain,interface,router,city,city_clli,airport_code,state,others) values(?,?,?,?,?,?,?,?,?)");
		pstmt.setString(1, isp);
		pstmt.setString(2, name.replaceAll(" ", "-")); //replaces the induced space
		pstmt.setString(3, "Domain");
		pstmt.setString(4, domain.replaceAll(" ", "-")); //replaces the induced space.
		pstmt.setString(5, domain.replaceAll(" ", "-"));
		pstmt.setInt(6, name.replaceAll(" ", "-").indexOf(domain.replaceAll(" ", "-")));
		
		pstmt.addBatch();
		if(!intf.equals(""))
		{
			for(String in : intf.split(":"))
			{
				String[] i = in.split("\\|");
				if(!i[0].equals("IGNORE"))
				{
					pstmt.setString(1, isp);
					pstmt.setString(2, name.replaceAll(" ", "-")); // replace the induced space
					pstmt.setString(3, "Interface");
					pstmt.setString(4, i[0]);
					pstmt.setString(5,i[0].replaceAll("\\d+$", "").replaceAll("^\\d+", ""));
					pstmt.setInt(6, new Integer(i[1]));
					pstmt.addBatch();
				}
			}
		}
		
		if(!router.equals(""))
		{
			for(String in : router.split(":"))
			{
				String[] i = in.split("\\|");
				if(!i[0].equals("IGNORE"))
				{
					pstmt.setString(1, isp);
					pstmt.setString(2, name.replaceAll(" ", "-")); //replaces the induced space
					pstmt.setString(3, "Router");
					pstmt.setString(4, i[0]);
					pstmt.setString(5, i[0].replaceAll("\\d+$","").replaceAll("^\\d+",""));
					pstmt.setInt(6, new Integer(i[1]));
					pstmt.addBatch();
				}
			}
		}
		
		//region bucket starts
		//Among city, city_clli and airport code, only one is the true geo-location of the interface. So, by observation, we have come up with certain rules.
		//put an entity in region bucket(by marking the corresponding flag as true) if it has the highest index.
		String[] citylist_tmp = city.split(":");
		String[] cityIndexArray = new String[city.length() - city.replaceAll(":", "").length()];
		int i=0;
		for(String c: citylist_tmp)
		{
			if (!c.equals(""))
			if(!citylist_tmp[i].split("\\|")[0].equals("IGNORE"))
				cityIndexArray[i] = citylist_tmp[i].split("\\|")[1];
			
			i++;	
		}
		
		String[] cityClliList_tmp = city_clli.split(":");
		String[] cityClliIndexArray = new String[city_clli.length() - city_clli.replaceAll(":", "").length()];
		i=0;
		for(String c: cityClliList_tmp)
		{
			if (!c.equals(""))
			if(!cityClliList_tmp[i].split("\\|")[0].equals("IGNORE"))
				cityClliIndexArray[i] = cityClliList_tmp[i].split("\\|")[1];
			
			i++;	
		}
		
		String[] arprtCodeList_tmp = airport_code.split(":");
		String[] arprtCodeIndexArray = new String[airport_code.length() - airport_code.replaceAll(":", "").length()];
		i=0;
		for(String c: arprtCodeList_tmp)
		{
			if (!c.equals(""))
			if(!arprtCodeList_tmp[i].split("\\|")[0].equals("IGNORE"))
				arprtCodeIndexArray[i] = arprtCodeList_tmp[i].split("\\|")[1];
			
			i++;	
		}
		
		if(cityClliIndexArray.length!=0) //consider only city, push the rest to others.
		{
			city_clli_reco =true;
		}
		else if(cityIndexArray.length!=0)
		{
			if (arprtCodeIndexArray.length!=0)
			{
				if(new Integer(cityIndexArray[cityIndexArray.length-1])> new Integer(arprtCodeIndexArray[arprtCodeIndexArray.length-1]))
				{
					city_reco = true;
				}
				else
					airport_code_reco=true;
			}
			else
			{
				city_reco = true;
			}
		}
		else
		{
			if(arprtCodeIndexArray.length!=0)
				airport_code_reco = true;
		}
		
		//region bucket ends
		
		if(!city.equals(""))
		{
			int num = city.split(":").length;
			int j=0;
			for(String in : city.split(":"))
			{
				String[] s = in.split("\\|");
				if(!s[0].equals("IGNORE"))
				{
					pstmt.setString(1, isp);
					pstmt.setString(2, name.replaceAll(" ", "-")); //replaces the induced space
					if(city_reco && j+1==num)
						pstmt.setString(3, "City(GeoLocation)");
					else
						pstmt.setString(3, "City");
					pstmt.setString(4, s[0]);
					pstmt.setString(5, s[0].replaceAll("\\d+$","").replaceAll("^\\d+",""));
					pstmt.setInt(6, new Integer(s[1]));
					pstmt.addBatch();
					j++;
				}
			}
		}
		
		if(!city_clli.equals(""))
		{
			int num = city_clli.split(":").length;
			int j=0;
			for(String in : city_clli.split(":"))
			{
				String[] s = in.split("\\|");
				if(!s[0].equals("IGNORE"))
				{
					pstmt.setString(1, isp);
					pstmt.setString(2, name.replaceAll(" ", "-")); //replaces the induced space
					if(city_clli_reco && j+1==num)
						pstmt.setString(3, "City Clli(GeoLocation)");
					else
						pstmt.setString(3, "City Clli");
					pstmt.setString(4, s[0]);
					pstmt.setString(5, s[0].replaceAll("\\d+$","").replaceAll("^\\d+",""));
					pstmt.setInt(6, new Integer(s[1]));
					pstmt.addBatch();
					j++;
				}
			}
		}
		
		if(!airport_code.equals(""))
		{
			int num = airport_code.split(":").length;
			int j=0;
			
			for(String in : airport_code.split(":"))
			{
				String[] s = in.split("\\|");
				if(!s[0].equals("IGNORE"))
				{
					pstmt.setString(1, isp);
					pstmt.setString(2, name.replaceAll(" ", "-")); //replaces the induced space
					if(airport_code_reco && j+1==num)
						pstmt.setString(3, "Airport Code(GeoLocation)");
					else
						pstmt.setString(3, "Airport Code");
					pstmt.setString(4, s[0]);
					pstmt.setString(5, s[0].replaceAll("\\d+$","").replaceAll("^\\d+",""));
					pstmt.setInt(6, new Integer(s[1]));
					pstmt.addBatch();
					j++;
				}
			}
		}
		
		if(!state_decode.equals(""))
		{
			for(String in : state_decode.split(":"))
			{
				String[] s = in.split("\\|");
				if(!s[0].equals("IGNORE"))
				{
					pstmt.setString(1, isp);
					pstmt.setString(2, name.replaceAll(" ", "-")); //replaces the induced space
					pstmt.setString(3, "State");
					pstmt.setString(4, s[0]);
					pstmt.setString(5, s[0].replaceAll("\\d+$","").replaceAll("^\\d+",""));
					pstmt.setInt(6, new Integer(s[1]));
					pstmt.addBatch();
				}
			}
		}		
			
		checkDictionaryAndInsert(ip,name,other);
		
	}
	
	private void resetall()
	{
		domain="";intf="";router="";airport_code="";city="";state_decode="";city_clli="";other="";
		intfCounter=routerCounter=airport_codeCounter=city_clliCounter=cityCounter=airport_codeCounter=state_decodeCounter=othersCounter=0;
		intf_found = router_found= airport_code_found =city_found= state_found = city_clli_found =-1;
	}
	
	private void partSplit(String name, int len_domain)
	{
		String[] parts = name.split("\\.");
		int size=parts.length,count=0;
		for (String part : parts) 
		{
			if (count++<size-len_domain)
			{	
				if (ProcessDnsNames_batch.cityMap.containsKey(part.replaceAll("\\d+$","").replaceAll("^\\d+","")))
				{
					if(part.contains("-"))
					{
						for (String splitDash : part.split("-")) 
						{
							for (String intf_i : intf.split(":")) {
								if (splitDash.equals(intf_i.split("\\|")[0]))
								{
									if(intf.indexOf(":"+intf_i.split("\\|")[0]) !=-1)
										intf = intf.replaceAll(":"+intf_i.split("\\|")[0]+"\\|", ":IGNORE|");
									else
										intf = intf.replaceAll("^"+intf_i.split("\\|")[0]+"\\|", "IGNORE|");
								}
							}

							for (String router_i : router.split(":")) {
								if (splitDash.equals(router_i.split("\\|")[0]))
								{
									if(router.indexOf(":"+router_i.split("\\|")[0]) !=-1)
										router = router.replaceAll(":"+router_i.split("\\|")[0]+"\\|", ":IGNORE|");
									else
										router = router.replaceAll("^"+router_i.split("\\|")[0]+"\\|", "IGNORE|");
								}
							}

							for (String city_i : city.split(":")) {
								if (splitDash.equals(city_i.split("\\|")[0]))
								{
									if(city.indexOf(":"+city_i.split("\\|")[0]) !=-1)
										city = city.replaceAll(":"+city_i.split("\\|")[0]+"\\|", ":IGNORE|");
									else
										city = city.replaceAll("^"+city_i.split("\\|")[0]+"\\|", "IGNORE|");
								}
							}

							for (String city_clli_i : city_clli.split(":")) {
								if (splitDash.equals(city_clli_i.split("\\|")[0]))
								{
									if(city_clli.indexOf(":"+city_clli_i.split("\\|")[0]) !=-1)
										city_clli = city_clli.replaceAll(":"+city_clli_i.split("\\|")[0]+"\\|", ":IGNORE|");
									else
										city_clli = city_clli.replaceAll("^"+city_clli_i.split("\\|")[0]+"\\|", "IGNORE|");
								}
							}

							for (String airport_code_i : airport_code.split(":")) {
								if (splitDash.equals(airport_code_i.split("\\|")[0]))
								{
									if(airport_code.indexOf(":"+airport_code_i.split("\\|")[0]) !=-1)
										airport_code = airport_code.replaceAll(":"+airport_code_i.split("\\|")[0]+"\\|", ":IGNORE|");
									else
										airport_code = airport_code.replaceAll("^"+airport_code_i.split("\\|")[0]+"\\|", "IGNORE|");
								}
							}
							
							for (String state_i : state_decode.split(":")) {
								if (splitDash.equals(state_i.split("\\|")[0]))
								{
									if(state_decode.indexOf(":"+state_i.split("\\|")[0]) !=-1)
										state_decode = state_decode.replaceAll(":"+state_i.split("\\|")[0]+"\\|", ":IGNORE|");
									else
										state_decode = state_decode.replaceAll("^"+state_i.split("\\|")[0]+"\\|", "IGNORE|");
								}
							}
							
							for (String other_i : other.split(":")) {
								if (splitDash.equals(other_i.split("\\|")[0]))
								{
									if(other.indexOf(":"+other_i.split("\\|")[0]) !=-1)
										other = other.replaceAll(":"+other_i.split("\\|")[0]+"\\|", ":IGNORE|");
									else
										other = other.replaceAll("^"+other_i.split("\\|")[0]+"\\|", "IGNORE|");
								}
							}
						}
						if (!city.contains(part.replaceAll("\\d+$","").replaceAll("^\\d+","")))
							city_found = name.indexOf(part);
						
						city = city + part +"|"+city_found+":";
					}
				}
			}
		}
   }
}


public class ProcessDnsNames_batch {

	static ConcurrentHashMap<String, String> intfMap = new ConcurrentHashMap<String, String>();
	static ConcurrentHashMap<String, String> routerMap = new ConcurrentHashMap<String, String>();
	static ConcurrentHashMap<String, String> state_decodeMap = new ConcurrentHashMap<String, String>();
	static ConcurrentHashMap<String, String> airport_code_decodeMap = new ConcurrentHashMap<String, String>();
	static ConcurrentHashMap<String, String> city_clliDecodeMap = new ConcurrentHashMap<String, String>();
	static ConcurrentHashMap<String, String> cityMap = new ConcurrentHashMap<String, String>();
	static ConcurrentHashMap<String, String> dictMap = new ConcurrentHashMap<String, String>();
	String a;
	
	public static void main(String[] args) throws SQLException, InterruptedException {

		PreparedStatement pstmt1 = DataBaseManager.getInstance().getConn().prepareStatement("select * from interface_decode_tmp");
		ResultSet rs1 = pstmt1.executeQuery();
		while(rs1.next())
		{
			intfMap.put(rs1.getString("code").toLowerCase(),rs1.getString("value"));
		}
		rs1.close();
		
		PreparedStatement pstmt2 = DataBaseManager.getConn().prepareStatement("select * from router_decode_tmp");
		ResultSet rs2 = pstmt2.executeQuery();
		while(rs2.next())
		{
			routerMap.put(rs2.getString("code").toLowerCase(),rs2.getString("value"));
		}
		
		rs2.close();
		PreparedStatement pstmt3 = DataBaseManager.getConn().prepareStatement("select * from state_decode");
		ResultSet rs3 = pstmt3.executeQuery();
		
		while(rs3.next())
		{
			state_decodeMap.put(rs3.getString("code").toLowerCase(),rs3.getString("value"));
		}
		rs3.close();
		
		PreparedStatement pstmt4 = DataBaseManager.getConn().prepareStatement("select * from city_decode");
		ResultSet rs4 = pstmt4.executeQuery();
		
		while(rs4.next())
		{
			airport_code_decodeMap.put(rs4.getString("code").toLowerCase(),rs4.getString("value"));
		}
		rs4.close();
		
		PreparedStatement pstmt6 = DataBaseManager.getConn().prepareStatement("select t.* from city_clli t where code not in('VERNAL','ANDRIA','LEONIA','AUSTIN','FLORIN','CHESMA','OSTROH','LONDON','LYNDON','MALTON')");
        ResultSet rs6 = pstmt6.executeQuery();
		
		while(rs6.next())
		{
			city_clliDecodeMap.put(rs6.getString("code").toLowerCase(),rs6.getString("value"));
		}
		rs6.close();
		
		PreparedStatement pstmt7 = DataBaseManager.getConn().prepareStatement("select * from geonames5000 where length(asciiname)>3");
		ResultSet rs7 = pstmt7.executeQuery();
		
		while(rs7.next())
		{
			cityMap.put(rs7.getString("asciiname").toLowerCase(),"1");
		}
		rs7.close();
		
		PreparedStatement pstmt10 = DataBaseManager.getConn().prepareStatement("select lft from wordnet.xwnparselfts where lft not regexp '^[a-z]{2}:.*' and lft not regexp '^[a-z]*-.*' and lft not regexp '^[a-z]*_.*'");
		ResultSet rs10 = pstmt10.executeQuery();
		
		while(rs10.next())
		{
			if(rs10.getString(1).split(":")[0].length()!=1)
				dictMap.put(rs10.getString(1).split(":")[0],"1" );
		}
		rs10.close();
		
		PreparedStatement pstmt5 = DataBaseManager.getConn().prepareStatement("select distinct dnsname from "+args[0]+" where dnsname!='' and dnsname!='IP Error' ");
		PreparedStatement pstmt8 = DataBaseManager.getConn().prepareStatement("update "+args[0]+" set pickedup='Y' where ipaddr=?"); 
		while(true)
		{
			int count=0;
			
			ResultSet rs5 = pstmt5.executeQuery();
			if(rs5.next()==false)
				break;
			else
				rs5.beforeFirst();
			
			//ArrayList<String> iplist = new ArrayList<String>();
			//ArrayList<String> ip = new ArrayList<String>();
			ArrayList<String> ips = new ArrayList<String>();
			ArrayList<Thread> threadList = new ArrayList<Thread>();
			while(rs5.next())
			{
				//ip.add(rs5.getString("ipaddr"));
				//iplist.add(rs5.getString("ipaddr"));
				ips.add(rs5.getString("dnsname"));
				//ips.add("101.234.25.8.res.dyn.sal.fibrant.com.");
				//ips.add("yogistic.hooknosedcoupon.com.");
				if(++count==100000/100){
					threadList.add(new Thread(new ExecProcessDnsNames(/*ip.toArray(new String[ip.size()]),*/ips.toArray(new String[ips.size()]),args[1] )));
					count=0;
					ips = new ArrayList<String>();
				}
				//break;
			}
			if(ips.size()>0)
				threadList.add(new Thread(new ExecProcessDnsNames(/*ip.toArray(new String[ip.size()]),*/ips.toArray(new String[ips.size()]),args[1])));
			
			rs5.close();
			for(Thread t : threadList)
				t.start();
			for(Thread t : threadList)
				t.join();
			
//			for(String i: iplist)
//			{
//				pstmt8.setString(1, i);
//				pstmt8.addBatch();
//			}
//			pstmt8.executeBatch();
			break;
		}//end of infinite loop
		PreparedStatement pstmt9 =DataBaseManager.getConn().prepareStatement("update "+args[0]+" set pickedup='N'");
		pstmt9.executeUpdate();
	}
}
