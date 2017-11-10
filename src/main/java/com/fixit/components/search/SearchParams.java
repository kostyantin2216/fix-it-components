/**
 * 
 */
package com.fixit.components.search;

import com.fixit.core.data.mongo.MapArea;
import com.fixit.core.data.sql.Profession;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/09 14:25:08 GMT+3
 */
public class SearchParams {
	
	public final Profession profession;
	public final MapArea location;
	
	public SearchParams(Profession profession, MapArea location) {
		this.profession = profession;
		this.location = location;
	}
}
