import { html } from "htm/preact"

export function Dialog({message, buttons}) {
    return html`<div class="dialog">
        <div class="dialog-message">
          ${message}
        </div>
        <ul class="dialog-buttons">
        ${buttons.map((option) => html`<li class="dialog-button">
          <button onclick=${() => option.onSelect()}>${option.label}</button>
        </li>`)}
        </ul>
    </div>`
}