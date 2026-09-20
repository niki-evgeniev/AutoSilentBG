<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:s="http://www.sitemaps.org/schemas/sitemap/0.9"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="s xhtml">
    <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes"/>

    <xsl:template match="/">
        <html lang="en">
        <head>
            <meta charset="UTF-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            <title>AutoSilent.bg Sitemap</title>
            <style>
                :root {
                    color-scheme: light;
                    font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                    color: #172033;
                    background: #f4f6f9;
                }

                * { box-sizing: border-box; }

                body {
                    margin: 0;
                    background: #f4f6f9;
                    line-height: 1.5;
                }

                main {
                    width: min(1180px, calc(100% - 32px));
                    margin: 48px auto;
                }

                .hero {
                    margin-bottom: 24px;
                    padding: 28px 30px;
                    border: 1px solid #e2e7ef;
                    border-radius: 18px;
                    background: #ffffff;
                    box-shadow: 0 12px 35px rgba(31, 45, 70, 0.07);
                }

                h1 {
                    margin: 0 0 8px;
                    font-size: clamp(1.75rem, 4vw, 2.5rem);
                    letter-spacing: -0.035em;
                }

                .intro { margin: 0; color: #5c667a; }

                .count {
                    display: inline-flex;
                    margin-top: 18px;
                    padding: 7px 12px;
                    border-radius: 999px;
                    color: #8a3510;
                    background: #fff0e8;
                    font-size: 0.875rem;
                    font-weight: 700;
                }

                .table-wrap {
                    overflow-x: auto;
                    border: 1px solid #e2e7ef;
                    border-radius: 18px;
                    background: #ffffff;
                    box-shadow: 0 12px 35px rgba(31, 45, 70, 0.07);
                }

                table { width: 100%; min-width: 760px; border-collapse: collapse; }

                th, td {
                    padding: 16px 18px;
                    border-bottom: 1px solid #edf0f5;
                    text-align: left;
                    vertical-align: top;
                }

                th {
                    color: #687286;
                    background: #fafbfc;
                    font-size: 0.75rem;
                    letter-spacing: 0.08em;
                    text-transform: uppercase;
                }

                tbody tr:last-child td { border-bottom: 0; }
                tbody tr:hover { background: #fffaf7; }

                a { color: #b94717; text-decoration: none; }
                a:hover { color: #7d2c0d; text-decoration: underline; }

                .url-link {
                    display: block;
                    max-width: 680px;
                    overflow-wrap: anywhere;
                    word-break: break-word;
                    font-weight: 650;
                }

                .date, .empty { color: #727d91; white-space: nowrap; }

                .alternates {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 7px;
                }

                .alternate {
                    display: inline-flex;
                    padding: 4px 9px;
                    border: 1px solid #f2c8b5;
                    border-radius: 999px;
                    background: #fff7f2;
                    font-size: 0.78rem;
                    font-weight: 750;
                }

                @media (max-width: 640px) {
                    main { width: min(100% - 20px, 1180px); margin: 20px auto; }
                    .hero { padding: 22px 20px; border-radius: 14px; }
                    .table-wrap { border-radius: 14px; }
                    th, td { padding: 13px 14px; }
                }
            </style>
        </head>
        <body>
        <main>
            <section class="hero">
                <h1>AutoSilent.bg Sitemap</h1>
                <p class="intro">This XML sitemap helps search engines discover the public pages on AutoSilent.bg.</p>
                <span class="count">
                    <xsl:value-of select="count(s:urlset/s:url)"/>
                    <xsl:text> URLs</xsl:text>
                </span>
            </section>

            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>URL</th>
                        <th>Last Modified</th>
                        <th>Language Alternates</th>
                    </tr>
                    </thead>
                    <tbody>
                    <xsl:for-each select="s:urlset/s:url">
                        <tr>
                            <td>
                                <a class="url-link" href="{s:loc}">
                                    <xsl:value-of select="s:loc"/>
                                </a>
                            </td>
                            <td>
                                <xsl:choose>
                                    <xsl:when test="s:lastmod">
                                        <span class="date"><xsl:value-of select="s:lastmod"/></span>
                                    </xsl:when>
                                    <xsl:otherwise><span class="empty">—</span></xsl:otherwise>
                                </xsl:choose>
                            </td>
                            <td>
                                <xsl:choose>
                                    <xsl:when test="xhtml:link[@rel='alternate']">
                                        <div class="alternates">
                                            <xsl:for-each select="xhtml:link[@rel='alternate']">
                                                <a class="alternate" href="{@href}">
                                                    <xsl:choose>
                                                        <xsl:when test="@hreflang='bg'">BG</xsl:when>
                                                        <xsl:when test="@hreflang='en'">EN</xsl:when>
                                                        <xsl:when test="@hreflang='x-default'">Default</xsl:when>
                                                        <xsl:otherwise>
                                                            <xsl:value-of select="@hreflang"/>
                                                        </xsl:otherwise>
                                                    </xsl:choose>
                                                </a>
                                            </xsl:for-each>
                                        </div>
                                    </xsl:when>
                                    <xsl:otherwise><span class="empty">—</span></xsl:otherwise>
                                </xsl:choose>
                            </td>
                        </tr>
                    </xsl:for-each>
                    </tbody>
                </table>
            </div>
        </main>
        </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
