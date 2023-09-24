import { html } from 'https://esm.sh/htm/preact'
import { useState } from 'https://esm.sh/preact/hooks'

function Difference({difference}) {
    if (!difference) return null;
    if (difference > 0) {
        return html`<div class="player-isk-difference player-isk-increase">${difference}</div>`
    } else {
        return html`<div class="player-isk-difference player-isk-decrease">${-difference}</div>`
    }
}

export function PlayerIsk({amount}) {
    const [previousAmount, setPreviousAmount] = useState(null)
    const [difference, setDifference] = useState(0)
    if (previousAmount != null && amount != previousAmount) {
        setDifference(amount - previousAmount);
        setTimeout(() => setDifference(0), 1000);
    }
    setPreviousAmount(amount);
    return html`<div class="player-isk">${amount}<${Difference} difference=${difference} /></div>`
}