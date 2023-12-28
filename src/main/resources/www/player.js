import { html } from "htm/preact"
import { Isk } from "./isk.js"

export function Player({ player, current, active }) {
    return html`
    <div class="player ${active ? "player-turn" : ""} ${current ? "player-you": ""}">
      <div class="player-name">${current ? html`<strong>You</strong>` : player.name}</div>
      <${Isk} amount=${player.isk} />
      <div class="player-influences">
        <${Influences} influences=${player.revealedInfluences} />
        <${Influences} influences=${player.heldInfluences} hidden />
      </div>
    </div>`
}

function Influence({ children, hidden }) {
    if (hidden) {
        return html`<div class="player-influence player-influence-hidden" data-type=${children}>${children}</div>`
    } else {
        return html`<div class="player-influence player-influence-revealed" data-type=${children}>${children}</div>`
    }
}

function Influences({ influences, hidden }) {
    if (typeof influences === "number") {
        return [...new Array(influences)].map(() => html`<${Influence} hidden=${hidden} />`)
    } else {
        return influences.map(influence => html`<${Influence} hidden=${hidden}>${influence}<//>`)
    }
}