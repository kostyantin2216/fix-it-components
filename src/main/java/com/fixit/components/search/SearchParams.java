/**
 * 
 */
package com.fixit.components.search;

import com.fixit.core.data.mongo.MapArea;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/04/09 14:25:08 GMT+3
 */
public class SearchParams {
	
	public final int professionId;
	public final MapArea location;
	
	public SearchParams(int professionId, MapArea location) {
		this.professionId = professionId;
		this.location = location;
	}
}
