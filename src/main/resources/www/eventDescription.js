export function describe({ event, type }, players) {
    function player(number) {
        return players?.[number === undefined ? event.player : number]?.name
    }

    switch (type) {
        case "ActionAttempted": return `${player()} ${attempted(event.actionType, player(event.target))}.`
        case "ActionChallenged": return `${player(event.challenger)} challenged ${player()}'s claim of influence with ${the(event.neededInfluence)}.`
        case "ActionPerformed": return `${player()} ${performed(event.actionType, player(event.target))}.`
        case "BlockAttempted": return `${player(event.blocker)} attempted to block ${player()}'s ${attemptOf(event.actionType)} on behalf of ${the(event.influence)}.`
        case "BlockChallenged": return `${player(event.challenger)} challenged ${player(event.blocker)}'s claim of influence with ${the(event.influence)}.`
        case "ActionBlocked": return `${player(event.blocker)} blocked ${player()}'s ${attemptOf(event.actionType)} on behalf of ${the(event.influence)}.`
        case "GameOver": return `${player(event.winner)} won the round!`
        case "InfluenceRevealed": return `${player()} revealed ${the(event.influence)}.`
        case "InfluenceSurrendered": return `${player()} surrendered their influence with ${the(event.influence)}.`
        case "TurnStarted": return `${player()}'s turn has started.`

    }
}

function performed(actionType, target) {
    switch (actionType) {
        case "Income": return "took income"
        case "Foreign Aid": return "accepted foreign aid"
        case "Tax": return "collected taxes"
        case "Steal": return `stole from ${target}`
        case "Exchange": return "exchanged with the court"
        case "Assassinate": return `assassinated ${target}`
        case "Coup": return `performed a coup against ${target}`
    }
}

function attempted(actionType, target) {
    switch (actionType) {
        case "Foreign Aid": return "invoked foreign aid"
        case "Tax": return "tried to collect taxes"
        case "Steal": return `tried to steal from ${target}`
        case "Exchange": return "tried to exchange with the court"
        case "Assassinate": return `tried to assassinate ${target}`
        case "Income": return "attempted to collect income"
        case "Coup": return `attempted a coup against ${target}`
    }
}

function attemptOf(actionType) {
    switch (actionType) {
        case "Foreign Aid": return "foreign aid"
        case "Tax": return "taxes"
        case "Steal": return "theft"
        case "Exchange": return "exchange"
        case "Assassinate": return "assassination"
        case "Income": return "income"
        case "Coup": return "coup"
    }
}

function the(influence) {
    return `the ${influence}`
}
