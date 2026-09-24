package com.hagenthon.session730;

import com.hagenthon.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class SummaryGeneratorService {

    @SuppressWarnings("unchecked")
    public String generateHtml(Map<String, Object> summary, User user) {
        log.info("SummaryGeneratorService.generateHtml: generazione riepilogo HTML per utente={} sessione={}",
                user.getEmail(), summary.get("sessionId"));
        Map<String, String> values = (Map<String, String>) summary.getOrDefault("values", Map.of());

        StringBuilder rows = new StringBuilder();
        for (TrecentoStep step : TrecentoStep.values()) {
            String value = values.getOrDefault(step.name(), null);
            String displayValue = (value != null && !value.isBlank())
                    ? escapeHtml(value)
                    : "<em style='color:#999'>Non compilato</em>";
            rows.append("""
                            <tr>
                                <td class="num">%d</td>
                                <td>%s</td>
                                <td>%s</td>
                            </tr>
                    """.formatted(step.getIndex() + 1, escapeHtml(step.getDisplayName()), displayValue));
        }

        return """
                <!DOCTYPE html>
                <html lang="it">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Riepilogo 730 - 730 Facile</title>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body { font-family: 'Segoe UI', Arial, sans-serif; background: #f5f5f5;
                               color: #333; padding: 30px; }
                        .container { max-width: 820px; margin: 0 auto; background: #fff;
                                     border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.12);
                                     padding: 32px; }
                        h1 { color: #1a4fa0; font-size: 1.6rem; margin-bottom: 6px; }
                        .subtitle { color: #666; font-size: .9rem; margin-bottom: 24px; }
                        .meta { background: #f0f4ff; border-left: 4px solid #1a4fa0;
                                padding: 12px 16px; border-radius: 4px; margin-bottom: 24px;
                                font-size: .92rem; line-height: 1.7; }
                        table { width: 100%%; border-collapse: collapse; margin-bottom: 24px; }
                        th { background: #1a4fa0; color: #fff; padding: 10px 14px;
                             text-align: left; font-size: .9rem; }
                        td { padding: 10px 14px; border-bottom: 1px solid #e8e8e8;
                             font-size: .9rem; vertical-align: top; }
                        td.num { color: #888; width: 40px; }
                        tr:nth-child(even) td { background: #fafafa; }
                        .disclaimer { background: #fffbe6; border: 1px solid #ffe066;
                                      padding: 14px 16px; border-radius: 6px;
                                      font-size: .85rem; line-height: 1.6; }
                        .disclaimer strong { color: #b8860b; }
                        .footer { margin-top: 20px; text-align: center;
                                  color: #aaa; font-size: .78rem; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>Riepilogo Dichiarazione 730</h1>
                        <p class="subtitle">730 Facile &mdash; Assistente alla verifica del precompilato</p>
                        <div class="meta">
                            <strong>Contribuente:</strong> %s &lt;%s&gt;<br>
                            <strong>Sessione:</strong> %s<br>
                            <strong>Data:</strong> %s<br>
                            <strong>Stato:</strong> %s
                        </div>
                        <table>
                            <thead>
                                <tr>
                                    <th>#</th>
                                    <th>Campo Fiscale</th>
                                    <th>Valore Confermato</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                        <div class="disclaimer">
                            <strong>Disclaimer:</strong> Questo riepilogo &egrave; generato da 730 Facile a scopo puramente informativo.
                            I valori riportati sono stati estratti automaticamente da documenti caricati dall&rsquo;utente e confermati
                            da quest&rsquo;ultimo. 730 Facile non si assume alcuna responsabilit&agrave; per errori od omissioni.
                            Si raccomanda di verificare tutti i dati con un professionista fiscale qualificato prima
                            della presentazione definitiva della dichiarazione dei redditi.
                        </div>
                        <div class="footer">Generato da 730 Facile &copy; 2024 &mdash; Solo uso locale</div>
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(user.getNome()),
                escapeHtml(user.getEmail()),
                summary.get("sessionId"),
                summary.get("createdAt"),
                summary.get("status"),
                rows.toString()
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
