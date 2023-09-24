import { html } from 'https://esm.sh/htm/preact'

export function TurnIndicator({ thisPlayer, activePlayer }) {
    if (!activePlayer) return null;
    if (activePlayer.number === thisPlayer.number) {
        return html`<div class="turn-indicator">It's your turn</div>`
    } else {
        return html`<div class="turn-indicator">It's ${activePlayer.name}'s turn</div>`
    }
}