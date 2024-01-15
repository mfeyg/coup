import { html } from "htm/preact"

export function Isk({amount}) {
    return html`<div class="isk" style="--number: ${amount}">${amount} ISK</div>`
}