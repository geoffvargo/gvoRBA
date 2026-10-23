package com.geoffvargo.gvorbabackend.notification;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.*;
import org.springframework.core.io.support.*;
import org.thymeleaf.context.*;
import org.thymeleaf.spring6.*;
import org.thymeleaf.templateresolver.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TailwindInlinerTest {
	/// Small stand-in for the generated tailwind.json. p-6 comes before m-0 on purpose, to check
	/// that CSS order (not class-attribute order) decides which style comes first.
	private static final String RULES = """
		{
		  "rules": [
		    { "class": "p-6", "style": "padding: 24px" },
		    { "class": "m-0", "style": "margin: 0" },
		    { "class": "sm:p-2", "style": "padding: 8px" }
		  ],
		  "extraCss": "@media (min-width: 640px) { .sm\\\\:p-2 { padding: 8px } }"
		}
		""";

	private TailwindInliner inliner;

	@BeforeEach
	void setUp() {
		inliner = new TailwindInliner(new ByteArrayInputStream(RULES.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void inline_writesClassStylesInCssOrder_andKeepsExistingStyleLast() {
		String html = inliner.inline("<html><body class=\"m-0 p-6\" style=\"color: red;\"></body></html>");

		assertEquals("padding: 24px; margin: 0; color: red", Jsoup.parse(html).body().attr("style"));
	}

	@Test
	void inline_leavesUnknownClassesAndUnstyledElementsAlone() {
		String html = inliner.inline("<html><body><p class=\"not-tailwind\">hi</p><span>x</span></body></html>");
		Document doc = Jsoup.parse(html);

		assertFalse(doc.selectFirst("p").hasAttr("style"));
		assertEquals("not-tailwind", doc.selectFirst("p").className());
		assertFalse(doc.selectFirst("span").hasAttr("style"));
	}

	@Test
	void inline_putsMediaRulesInHeadStyleTag() {
		String html = inliner.inline("<html><head></head><body class=\"sm:p-2\"></body></html>");

		assertTrue(Jsoup.parse(html).head().selectFirst("style").data().contains("@media"));
	}

	@Test
	void inline_withoutRulesFile_returnsHtmlUnstyled() {
		String html = new TailwindInliner(null).inline("<html><body class=\"p-6\"></body></html>");

		assertFalse(Jsoup.parse(html).body().hasAttr("style"));
	}

	/// Renders the real template with the real committed tailwind.json.
	@Test
	void bookingCancelledTemplate_rendersWithEmailSafeInlineStyles_andNoPreviewScript() {
		String html = new TailwindInliner().inline(render("email/booking-cancelled"));
		String bodyStyle = Jsoup.parse(html).body().attr("style");

		assertTrue(bodyStyle.contains("margin: 0"), bodyStyle);
		assertTrue(bodyStyle.contains("padding: 24px"), bodyStyle);
		assertTrue(bodyStyle.contains("font-family:"), bodyStyle);
		assertFalse(html.contains("var("), html);
		assertFalse(html.contains("oklch("), html);
		/// The Tailwind browser script is for previewing the raw file only; th:remove must drop it.
		assertFalse(html.contains("<script"), html);
	}

	/// Fails when a template uses a class that isn't in tailwind.json, i.e. someone forgot to run
	/// `npm run email-css` in the frontend after editing a template.
	@Test
	void everyClassInEmailTemplates_hasACompiledStyle() throws IOException {
		TailwindInliner real = new TailwindInliner();
		Set<String> missing = new TreeSet<>();

		Resource[] templates = new PathMatchingResourcePatternResolver()
			.getResources("classpath*:templates/email/**/*.html");
		for (Resource template : templates) {
			Document doc = Jsoup.parse(template.getContentAsString(StandardCharsets.UTF_8));
			for (Element element : doc.select("[class]")) {
				for (String className : element.classNames()) {
					if (!real.hasRule(className)) {
						missing.add(template.getFilename() + ": " + className);
					}
				}
			}
		}

		assertTrue(missing.isEmpty(),
		           "Classes with no compiled style (run `npm run email-css` in frontend/gvorba-frontend): " + missing);
	}

	private static String render(String template) {
		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setPrefix("templates/");
		resolver.setSuffix(".html");
		/// SpringTemplateEngine, like the app: it evaluates ${...} with SpEL (plain TemplateEngine needs OGNL).
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.setTemplateResolver(resolver);
		return engine.process(template, new Context());
	}
}