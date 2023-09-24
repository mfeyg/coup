import { html } from 'https://esm.sh/htm/preact'

function Influence({ children, hidden }) {
    if (hidden) {
        return html`<div class="player-influence player-influence-hidden">${children}</div>`
    } else {
        return html`<div class="player-influence player-influence-revealed">${children}</div>`
    }
}

function Influences({ influences, hidden }) {
    console.debug(influences)
    if (typeof influences === "number") {
        return [...new Array(influences)].map(() => html`<${Influence} hidden=${hidden} />`)
    } else {
        return influences.map(influence => html`<${Influence} hidden=${hidden}>${influence}<//>`)
    }
}

export function Player({ player, current, active }) {
    return html`
    <div class="player ${active ? "player-turn" : ""}">
      <div class="player-name">${current ? html`<strong>You</strong>` : player.name}</div>
      <div class="player-isk">${player.isk}</div>
      <div class="player-influences">
        <${Influences} influences=${player.heldInfluences} hidden />
        <${Influences} influences=${player.revealedInfluences} />
      </div>
    </div>`
}