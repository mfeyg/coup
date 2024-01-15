import { html } from "htm/preact"

export function Isk(props) {
    const {amount} = props;
    return html`<div class="isk ${props.class}" style="--number: ${amount}">${amount} ISK</div>`
}