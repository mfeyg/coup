import { html } from "htm/preact"
import { useState } from "preact/hooks"

function Difference({difference}) {
    if (!difference) return null;
    if (difference > 0) {
        return html`<div class="isk-difference isk-increase">${difference}</div>`
    } else {
        return html`<div class="isk-difference isk-decrease">${-difference}</div>`
    }
}

export function Isk({amount}) {
    const [previousAmount, setPreviousAmount] = useState(null)
    const [difference, setDifference] = useState(0)
    if (previousAmount != null && amount != previousAmount) {
        setDifference(amount - previousAmount);
        setTimeout(() => setDifference(0), 1000);
    }
    setPreviousAmount(amount);
    return html`<div class="isk" style="--number: ${amount}">${amount} ISK<${Difference} difference=${difference} /></div>`
}