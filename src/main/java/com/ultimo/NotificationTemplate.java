package com.ultimo;

import org.jsoup.nodes.Document;

public interface NotificationTemplate {
	public Document createEmail(String content, String location, String template);
}
