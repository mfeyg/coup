import { html } from "htm/preact"

export function Overlay({connected}) {
    if (!connected) {
        return html`<div class="overlay"><h1>Disconnected</h1></div>`
    }
}