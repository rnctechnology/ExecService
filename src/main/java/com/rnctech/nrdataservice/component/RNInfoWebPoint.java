package com.rnctech.nrdataservice.component;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
//import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.rnctech.nrdataservice.ExcludeFromTest;
import com.rnctech.nrdataservice.utils.RNUtilities;

/**
 * @author zilin
 * @since 2020.06
 */

@Component
@ExcludeFromTest
@EndpointWebExtension(endpoint = InfoEndpoint.class)
public class RNInfoWebPoint {
   
   private final List<InfoContributor> infoContributors;

   
	public RNInfoWebPoint(List<InfoContributor> infoContributors) {
		Assert.notNull(infoContributors, "Info contributors must not be null");
		this.infoContributors = infoContributors;
	}

	@ReadOperation
	public Map<String, Object> info() {
		Info.Builder builder = new Info.Builder();
		for (InfoContributor contributor : this.infoContributors) {
			contributor.contribute(builder);
		}
		Info build = builder.build();
		Map<String, Object> omap = build.getDetails();
		return transferMap(omap);
	}

	private Map<String, Object> transferMap(Map<String, Object> omap){
		Map<String, Object> tmap = new HashMap<>();
		for(Map.Entry<String, Object> entry : omap.entrySet()) {
			if(entry.getValue() instanceof Instant) {
				long l = ((Instant)entry.getValue()).toEpochMilli();
				tmap.put(entry.getKey(), RNUtilities.getUTCTimeWithZone(l));
			}else if(entry.getValue() instanceof Date) {
				long l = ((Date)entry.getValue()).getTime();
				tmap.put(entry.getKey(), RNUtilities.getUTCTimeWithZone(l));	
			}else if(entry.getValue() instanceof Map){
				tmap.put(entry.getKey(), transferMap((Map)entry.getValue()));
			}else {
				tmap.put(entry.getKey(), entry.getValue());
			}
		}
		return tmap;
	}

}


