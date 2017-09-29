/**
 * 
 */
package com.fixit.components.synchronization.processors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fixit.core.dao.sql.ProfessionDao;
import com.fixit.core.data.sql.Profession;

/**
 * @author 		Kostyantin
 * @createdAt 	2017/03/30 21:45:41 GMT+3
 */
@Component
public class ProfessionProcessor extends BaseSynchronizationProcessor<ProfessionDao, Profession, Integer> {

	@Autowired
	public ProfessionProcessor(ProfessionDao professionDao) {
		super(professionDao);
	}

	@Override
	public String getDmoName() {
		return Profession.class.getSimpleName();
	}

}
