package com.rnctech.nrdataservice.component;

import java.util.Map;
import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;

import com.rnctech.nrdataservice.ExcludeFromTest;

/**
 * @author zilin
 * @since 2020.06
 */

@Component
@ExcludeFromTest
public class RNGitInfoContributor extends GitInfoContributor {

	public RNGitInfoContributor(GitProperties properties) {
		super(properties);
	}

	@Override
	public void contribute(Info.Builder builder) {
		Map<String, Object> map = generateContent();
		map.put("tag", getProperties().get("closest.tag.name"));
		map.put("build_user", getProperties().get("build.user.name"));
		Object cm = map.get("commit");
		String commituser = getProperties().get("commit.user.name");
		String commitmsg = getProperties().get("commit.message.short");
		if (null != cm && cm instanceof Map) {
			if (commituser != null) {
				((Map) cm).put("user", commituser);
			}
			if (commitmsg != null) {
				((Map) cm).put("message", commitmsg);
			}
		} else {
			if (commituser != null) {
				map.put("commit_user", commituser);
			}
			if (commitmsg != null) {
				map.put("commit_message", commitmsg);
			}
		}

		builder.withDetail("git", map);
	}
}
